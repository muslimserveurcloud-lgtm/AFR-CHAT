-- Premier administrateur : à exécuter UNE SEULE FOIS dans Supabase > SQL Editor,
-- après t'être inscrit dans l'app avec ton adresse e-mail.
update public.profiles
   set is_admin = true
 where id = (select id from auth.users where email = 'TON_EMAIL@exemple.com');
