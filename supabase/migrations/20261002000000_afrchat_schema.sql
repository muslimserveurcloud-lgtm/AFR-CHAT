-- ============================================================================
-- AFR CHAT — schéma Supabase (Postgres + RLS + Realtime + Storage)
-- Remplace Firestore, Firebase Auth, Storage, Functions.
-- Les horodatages sont en millisecondes epoch (bigint) pour coller aux modèles Kotlin.
-- ============================================================================

create or replace function public.now_ms() returns bigint
language sql as $$ select (extract(epoch from clock_timestamp()) * 1000)::bigint $$;

-- ============================================================================
-- PROFILS (1 ligne par compte auth.users)
-- ============================================================================
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  first_name text not null default '',
  last_name text not null default '',
  first_name_lower text generated always as (lower(first_name)) stored,
  last_name_lower text generated always as (lower(last_name)) stored,
  email text not null default '',
  phone text not null default '',
  photo_url text not null default '',
  status_message text not null default 'Salut, j''utilise AFR CHAT !',
  is_online boolean not null default false,
  last_seen bigint not null default 0,
  privacy jsonb not null default '{"showLastSeen":true,"showOnlineStatus":true,"showReadReceipts":true,"whoCanAddToGroups":"everyone"}'::jsonb,
  is_admin boolean not null default false,
  is_banned boolean not null default false,
  created_at bigint not null default public.now_ms()
);
create index if not exists profiles_first_name_lower_idx on public.profiles (first_name_lower text_pattern_ops);
create index if not exists profiles_last_name_lower_idx on public.profiles (last_name_lower text_pattern_ops);
create index if not exists profiles_phone_idx on public.profiles (phone text_pattern_ops);

-- Création automatique du profil à l'inscription (prénom/nom/téléphone passés en metadata).
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, first_name, last_name, email, phone)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'first_name', ''),
    coalesce(new.raw_user_meta_data->>'last_name', ''),
    '',  -- l'e-mail reste dans auth.users : profiles est lisible par tous les comptes connectés
    coalesce(new.raw_user_meta_data->>'phone', '')
  )
  on conflict (id) do nothing;
  return new;
end $$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- ============================================================================
-- CONVERSATIONS / MEMBRES
-- ============================================================================
create table if not exists public.conversations (
  id uuid primary key default gen_random_uuid(),
  type text not null default 'private' check (type in ('private', 'group')),
  group_id uuid,
  last_message text not null default '',
  last_message_type text not null default 'text',
  last_message_sender_id uuid,
  last_message_at bigint not null default public.now_ms(),
  created_at bigint not null default public.now_ms()
);

create table if not exists public.conversation_members (
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  unread_count integer not null default 0,
  is_archived boolean not null default false,
  is_muted boolean not null default false,
  is_typing boolean not null default false,
  primary key (conversation_id, user_id)
);
create index if not exists conversation_members_user_idx on public.conversation_members (user_id);

-- ============================================================================
-- MESSAGES
-- ============================================================================
create table if not exists public.messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.conversations(id) on delete cascade,
  sender_id uuid not null references public.profiles(id) on delete cascade,
  type text not null default 'text',
  text text not null default '',
  media_url text not null default '',
  media_duration_ms bigint not null default 0,
  file_name text not null default '',
  file_size_bytes bigint not null default 0,
  shared_contact_uid uuid,
  reply_to_message_id uuid,
  reactions jsonb not null default '{}'::jsonb,
  status text not null default 'sent',
  deleted_for uuid[] not null default '{}',
  is_deleted_for_everyone boolean not null default false,
  sent_at bigint not null default public.now_ms(),
  client_temp_id text not null default ''
);
create index if not exists messages_conv_sent_idx on public.messages (conversation_id, sent_at desc);
create index if not exists messages_sent_idx on public.messages (sent_at);
create unique index if not exists messages_dedupe_idx
  on public.messages (conversation_id, sender_id, client_temp_id) where client_temp_id <> '';

-- ============================================================================
-- GROUPES
-- ============================================================================
create table if not exists public.groups (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text not null default '',
  photo_url text not null default '',
  owner_id uuid not null references public.profiles(id),
  only_admins_can_post boolean not null default false,
  only_admins_can_edit_info boolean not null default true,
  conversation_id uuid references public.conversations(id) on delete set null,
  created_at bigint not null default public.now_ms()
);

create table if not exists public.group_members (
  group_id uuid not null references public.groups(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('owner', 'admin', 'member')),
  joined_at bigint not null default public.now_ms(),
  primary key (group_id, user_id)
);
create index if not exists group_members_user_idx on public.group_members (user_id);

do $$ begin
  alter table public.conversations
    add constraint conversations_group_fk foreign key (group_id) references public.groups(id) on delete set null;
exception when duplicate_object then null; end $$;

-- ============================================================================
-- STATUTS (stories), APPELS, SIGNALEMENTS
-- ============================================================================
create table if not exists public.stories (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  type text not null default 'text',
  content text not null default '',
  media_url text not null default '',
  background_color text not null default '#5B4FE9',
  viewer_ids uuid[] not null default '{}',
  created_at bigint not null default public.now_ms(),
  expires_at bigint not null default (public.now_ms() + 86400000)
);
create index if not exists stories_owner_idx on public.stories (owner_id, expires_at desc);

create table if not exists public.calls (
  id uuid primary key,
  caller_id uuid not null references public.profiles(id) on delete cascade,
  callee_id uuid not null references public.profiles(id) on delete cascade,
  is_video boolean not null default false,
  status text not null default 'ringing' check (status in ('ringing', 'accepted', 'declined', 'ended', 'missed')),
  offer_sdp text,
  answer_sdp text,
  created_at bigint not null default public.now_ms(),
  ended_at bigint
);
create index if not exists calls_caller_idx on public.calls (caller_id, created_at desc);
create index if not exists calls_callee_idx on public.calls (callee_id, created_at desc);

-- Pas de clé étrangère : l'appelant peut émettre ses candidats ICE avant que la ligne "calls" n'existe.
create table if not exists public.call_candidates (
  id bigint generated always as identity primary key,
  call_id uuid not null,
  from_role text not null check (from_role in ('caller', 'callee')),
  sdp_mid text not null default '',
  sdp_m_line_index integer not null default 0,
  candidate text not null default '',
  created_at bigint not null default public.now_ms()
);
create index if not exists call_candidates_call_idx on public.call_candidates (call_id, from_role);

create table if not exists public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  target_type text not null default 'user',
  target_id text not null default '',
  reason text not null default '',
  details text not null default '',
  status text not null default 'open' check (status in ('open', 'reviewed', 'dismissed')),
  created_at bigint not null default public.now_ms()
);

-- ============================================================================
-- FONCTIONS UTILITAIRES (security definer : évitent la récursion dans les politiques RLS)
-- ============================================================================
create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((select is_admin from public.profiles where id = auth.uid()), false)
$$;

create or replace function public.is_banned(uid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((select is_banned from public.profiles where id = uid), false)
$$;

create or replace function public.is_conversation_member(conv uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.conversation_members where conversation_id = conv and user_id = auth.uid())
$$;

create or replace function public.is_group_member(g uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.group_members where group_id = g and user_id = auth.uid())
$$;

create or replace function public.is_group_admin(g uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.group_members
                 where group_id = g and user_id = auth.uid() and role in ('owner', 'admin'))
$$;

create or replace function public.shares_conversation_with(other uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (
    select 1 from public.conversation_members a
    join public.conversation_members b on a.conversation_id = b.conversation_id
    where a.user_id = auth.uid() and b.user_id = other
  )
$$;

-- Peut-on publier dans cette conversation ? (groupes "seuls les admins peuvent publier")
create or replace function public.can_post(conv uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((
    select (not g.only_admins_can_post)
           or exists (select 1 from public.group_members gm
                      where gm.group_id = g.id and gm.user_id = auth.uid() and gm.role in ('owner', 'admin'))
    from public.conversations c join public.groups g on g.id = c.group_id
    where c.id = conv
  ), true)
$$;

-- ============================================================================
-- TRIGGER : à chaque message, met à jour l'aperçu de la conversation et les compteurs non-lus
-- ============================================================================
create or replace function public.on_message_insert() returns trigger
language plpgsql security definer set search_path = public as $$
declare preview text;
begin
  preview := case new.type
    when 'text' then new.text
    when 'image' then '📷 Photo'
    when 'video' then '🎥 Vidéo'
    when 'audio' then '🎤 Message vocal'
    when 'contact' then '👤 Contact : ' || new.text
    else '📎 ' || new.file_name
  end;
  update public.conversations
     set last_message = preview, last_message_type = new.type,
         last_message_sender_id = new.sender_id, last_message_at = new.sent_at
   where id = new.conversation_id;
  update public.conversation_members
     set unread_count = unread_count + 1
   where conversation_id = new.conversation_id and user_id <> new.sender_id;
  return new;
end $$;

drop trigger if exists messages_after_insert on public.messages;
create trigger messages_after_insert after insert on public.messages
  for each row execute function public.on_message_insert();

-- ============================================================================
-- RPC : CONVERSATIONS & MESSAGES
-- ============================================================================
create or replace function public.get_or_create_private_conversation(other_id uuid) returns uuid
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); cid uuid;
begin
  if me is null or public.is_banned(me) then raise exception 'not allowed'; end if;
  if other_id = me then raise exception 'invalid peer'; end if;

  select c.id into cid
    from public.conversations c
   where c.type = 'private'
     and exists (select 1 from public.conversation_members m where m.conversation_id = c.id and m.user_id = me)
     and exists (select 1 from public.conversation_members m where m.conversation_id = c.id and m.user_id = other_id)
     and (select count(*) from public.conversation_members m where m.conversation_id = c.id) = 2
   limit 1;
  if cid is not null then return cid; end if;

  insert into public.conversations (type) values ('private') returning id into cid;
  insert into public.conversation_members (conversation_id, user_id) values (cid, me), (cid, other_id);
  return cid;
end $$;

create or replace function public.mark_conversation_read(conv uuid) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_conversation_member(conv) then return; end if;
  update public.conversation_members set unread_count = 0 where conversation_id = conv and user_id = auth.uid();
  update public.messages set status = 'read'
   where conversation_id = conv and sender_id <> auth.uid() and status <> 'read';
end $$;

create or replace function public.react_to_message(msg uuid, emoji text) returns void
language sql security definer set search_path = public as $$
  update public.messages
     set reactions = case when emoji = '' then reactions - auth.uid()::text
                          else jsonb_set(reactions, array[auth.uid()::text], to_jsonb(emoji), true) end
   where id = msg and public.is_conversation_member(conversation_id)
$$;

create or replace function public.delete_message_for_me(msg uuid) returns void
language sql security definer set search_path = public as $$
  update public.messages set deleted_for = array_append(deleted_for, auth.uid())
   where id = msg and public.is_conversation_member(conversation_id) and not (auth.uid() = any (deleted_for))
$$;

create or replace function public.delete_message_for_everyone(msg uuid) returns void
language sql security definer set search_path = public as $$
  update public.messages set is_deleted_for_everyone = true, text = '', media_url = ''
   where id = msg and sender_id = auth.uid()
$$;

-- ============================================================================
-- RPC : GROUPES
-- ============================================================================
create or replace function public.create_group(p_name text, p_member_ids uuid[], p_photo_url text default '') returns uuid
language plpgsql security definer set search_path = public as $$
declare me uuid := auth.uid(); gid uuid; cid uuid; m uuid;
begin
  if me is null or public.is_banned(me) then raise exception 'not allowed'; end if;
  insert into public.groups (name, photo_url, owner_id) values (p_name, coalesce(p_photo_url, ''), me) returning id into gid;
  insert into public.conversations (type, group_id, last_message) values ('group', gid, 'Groupe créé') returning id into cid;
  update public.groups set conversation_id = cid where id = gid;
  insert into public.group_members (group_id, user_id, role) values (gid, me, 'owner');
  insert into public.conversation_members (conversation_id, user_id) values (cid, me);
  foreach m in array coalesce(p_member_ids, '{}') loop
    if m <> me then
      insert into public.group_members (group_id, user_id) values (gid, m) on conflict do nothing;
      insert into public.conversation_members (conversation_id, user_id) values (cid, m) on conflict do nothing;
    end if;
  end loop;
  return cid;
end $$;

create or replace function public.add_group_members(g uuid, uids uuid[]) returns void
language plpgsql security definer set search_path = public as $$
declare cid uuid; m uuid;
begin
  if not public.is_group_admin(g) then raise exception 'admin only'; end if;
  select conversation_id into cid from public.groups where id = g;
  foreach m in array coalesce(uids, '{}') loop
    insert into public.group_members (group_id, user_id) values (g, m) on conflict do nothing;
    insert into public.conversation_members (conversation_id, user_id) values (cid, m) on conflict do nothing;
  end loop;
end $$;

create or replace function public.remove_group_member(g uuid, uid uuid) returns void
language plpgsql security definer set search_path = public as $$
declare cid uuid;
begin
  if uid <> auth.uid() and not public.is_group_admin(g) then raise exception 'admin only'; end if;
  select conversation_id into cid from public.groups where id = g;
  delete from public.group_members where group_id = g and user_id = uid;
  delete from public.conversation_members where conversation_id = cid and user_id = uid;
end $$;

create or replace function public.set_group_admin(g uuid, uid uuid, make_admin boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not exists (select 1 from public.groups where id = g and owner_id = auth.uid()) then
    raise exception 'owner only';
  end if;
  update public.group_members set role = case when make_admin then 'admin' else 'member' end
   where group_id = g and user_id = uid and role <> 'owner';
end $$;

create or replace function public.update_group_info(g uuid, p_name text, p_description text, p_photo_url text) returns void
language plpgsql security definer set search_path = public as $$
declare only_admins boolean;
begin
  select only_admins_can_edit_info into only_admins from public.groups where id = g;
  if not (public.is_group_admin(g) or (public.is_group_member(g) and not coalesce(only_admins, true))) then
    raise exception 'not allowed';
  end if;
  update public.groups
     set name = coalesce(p_name, name),
         description = coalesce(p_description, description),
         photo_url = coalesce(p_photo_url, photo_url)
   where id = g;
end $$;

create or replace function public.set_only_admins_can_post(g uuid, v boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_group_admin(g) then raise exception 'admin only'; end if;
  update public.groups set only_admins_can_post = v where id = g;
end $$;

create or replace function public.mark_story_viewed(story uuid) returns void
language sql security definer set search_path = public as $$
  update public.stories set viewer_ids = array_append(viewer_ids, auth.uid())
   where id = story and (owner_id = auth.uid() or public.shares_conversation_with(owner_id))
     and not (auth.uid() = any (viewer_ids))
$$;

-- ============================================================================
-- RPC : ADMINISTRATION (remplace les Cloud Functions "callable")
-- ============================================================================
create or replace function public.ban_user(target_uid uuid, reason text default '') returns void
language plpgsql security definer set search_path = public, auth as $$
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.profiles set is_banned = true, is_online = false where id = target_uid;
  update auth.users set banned_until = now() + interval '100 years' where id = target_uid;
end $$;

create or replace function public.unban_user(target_uid uuid) returns void
language plpgsql security definer set search_path = public, auth as $$
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.profiles set is_banned = false where id = target_uid;
  update auth.users set banned_until = null where id = target_uid;
end $$;

create or replace function public.resolve_report(report_id uuid, new_status text) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  if new_status not in ('reviewed', 'dismissed') then raise exception 'invalid status'; end if;
  update public.reports set status = new_status where id = report_id;
end $$;

create or replace function public.set_admin_role(target_uid uuid, grant_admin boolean) returns void
language plpgsql security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  update public.profiles set is_admin = grant_admin where id = target_uid;
end $$;

create or replace function public.get_admin_stats() returns jsonb
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.is_admin() then raise exception 'admin only'; end if;
  return jsonb_build_object(
    'totalUsers', (select count(*) from public.profiles),
    'activeGroups', (select count(*) from public.groups),
    'openReports', (select count(*) from public.reports where status = 'open'),
    'messagesLast30Days', (select count(*) from public.messages where sent_at > public.now_ms() - 30::bigint * 86400000)
  );
end $$;

-- ============================================================================
-- SÉCURITÉ : RLS
-- ============================================================================
alter table public.profiles enable row level security;
alter table public.conversations enable row level security;
alter table public.conversation_members enable row level security;
alter table public.messages enable row level security;
alter table public.groups enable row level security;
alter table public.group_members enable row level security;
alter table public.stories enable row level security;
alter table public.calls enable row level security;
alter table public.call_candidates enable row level security;
alter table public.reports enable row level security;

-- profiles : lecture pour tout compte connecté ; modification de son propre profil seulement,
-- et jamais de is_admin / is_banned (verrouillés par privilèges de colonnes ci-dessous).
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles for select to authenticated using (true);
drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own on public.profiles for update to authenticated
  using (id = auth.uid() and not public.is_banned(auth.uid())) with check (id = auth.uid());

revoke insert, update, delete on public.profiles from anon, authenticated;
grant update (first_name, last_name, phone, photo_url, status_message, is_online, last_seen, privacy)
  on public.profiles to authenticated;

-- conversations : lecture par les membres ; toute écriture passe par les RPC / le trigger.
drop policy if exists conversations_select on public.conversations;
create policy conversations_select on public.conversations for select to authenticated
  using (public.is_conversation_member(id));
revoke insert, update, delete on public.conversations from anon, authenticated;

drop policy if exists members_select on public.conversation_members;
create policy members_select on public.conversation_members for select to authenticated
  using (user_id = auth.uid() or public.is_conversation_member(conversation_id));
drop policy if exists members_update_own on public.conversation_members;
create policy members_update_own on public.conversation_members for update to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid());
revoke insert, update, delete on public.conversation_members from anon, authenticated;
grant update (is_archived, is_muted, is_typing) on public.conversation_members to authenticated;

-- messages : lecture par les membres ; envoi en son propre nom ; pas d'UPDATE/DELETE direct.
drop policy if exists messages_select on public.messages;
create policy messages_select on public.messages for select to authenticated
  using (public.is_conversation_member(conversation_id));
drop policy if exists messages_insert on public.messages;
create policy messages_insert on public.messages for insert to authenticated
  with check (sender_id = auth.uid()
              and public.is_conversation_member(conversation_id)
              and not public.is_banned(auth.uid())
              and public.can_post(conversation_id));
revoke update, delete on public.messages from anon, authenticated;

-- groupes : lecture par les membres (ou admin global) ; écriture via RPC uniquement.
drop policy if exists groups_select on public.groups;
create policy groups_select on public.groups for select to authenticated
  using (public.is_group_member(id) or public.is_admin());
revoke insert, update, delete on public.groups from anon, authenticated;

drop policy if exists group_members_select on public.group_members;
create policy group_members_select on public.group_members for select to authenticated
  using (public.is_group_member(group_id) or public.is_admin());
revoke insert, update, delete on public.group_members from anon, authenticated;

-- stories : visibles par leur auteur et par les personnes avec qui il a une conversation, tant qu'elles n'ont pas expiré.
drop policy if exists stories_select on public.stories;
create policy stories_select on public.stories for select to authenticated
  using (owner_id = auth.uid()
         or (expires_at > public.now_ms() and public.shares_conversation_with(owner_id)));
drop policy if exists stories_insert on public.stories;
create policy stories_insert on public.stories for insert to authenticated
  with check (owner_id = auth.uid() and not public.is_banned(auth.uid()));
drop policy if exists stories_delete on public.stories;
create policy stories_delete on public.stories for delete to authenticated using (owner_id = auth.uid());
revoke update on public.stories from anon, authenticated;

-- appels
drop policy if exists calls_select on public.calls;
create policy calls_select on public.calls for select to authenticated
  using (auth.uid() in (caller_id, callee_id));
drop policy if exists calls_insert on public.calls;
create policy calls_insert on public.calls for insert to authenticated
  with check (caller_id = auth.uid() and not public.is_banned(auth.uid()));
drop policy if exists calls_update on public.calls;
create policy calls_update on public.calls for update to authenticated
  using (auth.uid() in (caller_id, callee_id)) with check (auth.uid() in (caller_id, callee_id));
revoke update on public.calls from anon, authenticated;
grant update (status, answer_sdp, ended_at) on public.calls to authenticated;

drop policy if exists call_candidates_select on public.call_candidates;
create policy call_candidates_select on public.call_candidates for select to authenticated
  using (exists (select 1 from public.calls c where c.id = call_id and auth.uid() in (c.caller_id, c.callee_id)));
drop policy if exists call_candidates_insert on public.call_candidates;
create policy call_candidates_insert on public.call_candidates for insert to authenticated with check (true);

-- signalements : création par tout compte, lecture/traitement réservés aux admins.
drop policy if exists reports_insert on public.reports;
create policy reports_insert on public.reports for insert to authenticated with check (reporter_id = auth.uid());
drop policy if exists reports_admin_select on public.reports;
create policy reports_admin_select on public.reports for select to authenticated using (public.is_admin());
revoke update, delete on public.reports from anon, authenticated;

-- Les RPC ne sont exécutables que par les comptes connectés.
do $$
declare f record;
begin
  for f in
    select p.oid::regprocedure as sig from pg_proc p join pg_namespace n on n.oid = p.pronamespace
     where n.nspname = 'public'
       and p.proname in ('get_or_create_private_conversation','mark_conversation_read','react_to_message',
         'delete_message_for_me','delete_message_for_everyone','create_group','add_group_members',
         'remove_group_member','set_group_admin','update_group_info','set_only_admins_can_post',
         'mark_story_viewed','ban_user','unban_user','resolve_report','set_admin_role','get_admin_stats')
  loop
    execute format('revoke all on function %s from public, anon', f.sig);
    execute format('grant execute on function %s to authenticated', f.sig);
  end loop;
end $$;

-- ============================================================================
-- REALTIME
-- ============================================================================
do $$
declare t text;
begin
  foreach t in array array['profiles','conversations','conversation_members','messages','groups',
                           'group_members','stories','calls','call_candidates']
  loop
    begin
      execute format('alter publication supabase_realtime add table public.%I', t);
    exception when duplicate_object then null;
    end;
  end loop;
end $$;

-- ============================================================================
-- STORAGE : un bucket public "media" (les URL contiennent un UUID impossible à deviner)
-- Chemin des fichiers : {dossier}/{uid}/{uuid}.{ext}  -> chacun n'écrit que dans son propre dossier.
-- ============================================================================
insert into storage.buckets (id, name, public, file_size_limit)
values ('media', 'media', true, 52428800)
on conflict (id) do update set public = true, file_size_limit = excluded.file_size_limit;

drop policy if exists media_insert_own on storage.objects;
create policy media_insert_own on storage.objects for insert to authenticated
  with check (bucket_id = 'media' and (storage.foldername(name))[2] = auth.uid()::text);
drop policy if exists media_delete_own on storage.objects;
create policy media_delete_own on storage.objects for delete to authenticated
  using (bucket_id = 'media' and (storage.foldername(name))[2] = auth.uid()::text);
drop policy if exists media_select on storage.objects;
create policy media_select on storage.objects for select using (bucket_id = 'media');

-- ============================================================================
-- NETTOYAGE DES STATUTS EXPIRÉS (toutes les heures, via pg_cron si disponible)
-- Note : les fichiers médias des statuts expirés restent dans le bucket (suppression SQL des
-- objets Storage déconseillée par Supabase).
-- ============================================================================
do $$
begin
  create extension if not exists pg_cron with schema pg_catalog;
  perform cron.schedule('afrchat-cleanup-stories', '0 * * * *',
    $job$ delete from public.stories where expires_at < (extract(epoch from now()) * 1000)::bigint $job$);
exception when others then
  raise notice 'pg_cron indisponible : active-le dans Database > Extensions pour purger les statuts expirés (%).', sqlerrm;
end $$;
