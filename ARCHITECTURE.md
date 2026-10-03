# Architecture AFR CHAT

## 1. Vue d'ensemble

```
Android (Kotlin, Jetpack Compose, MVVM)
        │
        ├── Supabase Auth        → comptes (e-mail / mot de passe), session persistée
        ├── Supabase Postgres    → données (PostgREST + fonctions RPC), sécurisées par RLS
        ├── Supabase Realtime    → flux temps réel (messages, conversations, appels, statuts)
        ├── Supabase Storage     → médias (bucket public "media", écriture limitée à son dossier)
        ├── pg_cron              → purge horaire des statuts expirés
        └── WebRTC (pair-à-pair) → flux audio/vidéo, signalisation via les tables calls / call_candidates
```

```
ui/{feature}/*Screen.kt        → Composables
ui/{feature}/*ViewModel.kt     → état UI (Hilt ViewModel)
data/repository/*Repository.kt → accès aux données (seule couche qui connaît Supabase)
data/remote/*                  → DTO Postgres + RealtimeHub
data/model/*.kt                → modèles de l'app (indépendants du backend)
```

Les ViewModels ne connaissent jamais Supabase : ils passent par les repositories.

## 2. Schéma (supabase/migrations)

| Table | Rôle |
|---|---|
| `profiles` | 1 ligne par compte (`id` = `auth.users.id`), créée par le trigger `handle_new_user`. Colonnes `first_name_lower` / `last_name_lower` générées pour la recherche par préfixe. |
| `conversations` + `conversation_members` | conversation (privée ou groupe) ; les compteurs non-lus, archivage, sourdine et « écrit… » sont **par membre**. |
| `messages` | un message par ligne ; le trigger `on_message_insert` met à jour l'aperçu de la conversation et les non-lus. Index unique anti-doublon sur `client_temp_id`. |
| `groups` + `group_members` | groupe et rôles (`owner` / `admin` / `member`). |
| `stories` | statuts, `expires_at` en ms, purgés chaque heure. |
| `calls` + `call_candidates` | signalisation WebRTC (offre/réponse SDP, candidats ICE). |
| `reports` | signalements, lisibles uniquement par les admins. |

Les horodatages sont des **millisecondes epoch** (`bigint`) pour coller aux modèles Kotlin.

## 3. Temps réel

`RealtimeHub` s'abonne aux changements Postgres des tables concernées ; à chaque événement, le
repository relance sa requête de lecture et émet le résultat dans un `Flow`. C'est volontairement
simple : un seul code de lecture, pas de reconstruction manuelle d'état à partir des événements.
La RLS s'applique aussi à Realtime : on ne reçoit que les changements des lignes qu'on a le droit de lire.

## 4. Sécurité

- **RLS activée sur toutes les tables.** Lecture limitée aux membres (conversations, messages, groupes),
  aux participants (appels) ou aux admins (signalements).
- **Écritures sensibles via RPC `security definer`** (création de groupe, rôles, bannissement,
  marquage « lu »…) qui revérifient les droits côté serveur. Les privilèges directs INSERT/UPDATE/DELETE
  sont révoqués sur ces tables.
- **Un utilisateur ne peut modifier sur `profiles` que** `first_name, last_name, phone, photo_url,
  status_message, is_online, last_seen, privacy` (privilèges de colonnes) : jamais `is_admin` / `is_banned`.
- **Bannissement** : `profiles.is_banned` + `auth.users.banned_until` (la session ne se rafraîchit plus).
- **Clé anon** dans l'APK : publique par conception. Ne jamais embarquer la clé `service_role`.
- **Storage** : un utilisateur ne peut écrire/supprimer que dans `{dossier}/{son uid}/…`. Le bucket est
  public en lecture : les URL contiennent un UUID impossible à deviner, mais quiconque possède une URL
  peut la lire. Pour des médias réellement privés, passer à un bucket privé + URL signées (non fait ici).
- **Chiffrement de bout en bout : non implémenté.** Ne jamais prétendre le contraire.

## 5. Appels audio/vidéo (WebRTC)

1. L'appelant crée une ligne `calls` (id généré côté app) avec son offre SDP.
2. Le destinataire, abonné via Realtime à ses appels « ringing » récents (< 1 min), voit l'écran d'appel
   entrant — **uniquement si l'app tourne** (pas de push).
3. Il répond : `status = accepted` + `answer_sdp` dans la même ligne.
4. Les candidats ICE passent par `call_candidates` (`from_role` = caller / callee).
5. Le flux audio/vidéo circule ensuite directement entre les deux téléphones. Un serveur TURN est
   nécessaire pour les réseaux restrictifs (voir README).

## 6. Notifications

Notifications locales (`MessageNotifier`) alimentées par Realtime : le contenu du message n'est jamais
affiché, seulement le nom de l'expéditeur. Pas de push app fermée (limitation Supabase, voir README).
