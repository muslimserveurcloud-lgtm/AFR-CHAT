# AFR CHAT

Application Android native de messagerie instantanée — Kotlin + Jetpack Compose + **Supabase** +
WebRTC. Architecture MVVM / Repository, identité visuelle originale (voir `app/src/main/res`).

> ⚠️ **À lire avant tout** : ce projet a été migré de Firebase vers Supabase dans un environnement
> sans accès réseau. **Le code n'a pas pu être compilé ni exécuté.** La migration utilise
> `supabase-kt 3.0.0` (Kotlin 2.0.21, Ktor 3.0.3, WebRTC 125.6422.07) ; si Gradle signale une erreur d'API ou de version, colle le message du log
> pour qu'on la corrige. Aucun APK n'a été généré automatiquement.

## Sommaire
1. Fonctionnalités · 2. Limitations · 3. Configurer Supabase · 4. Appels (TURN) ·
5. Lancer le projet · 6. GitHub Actions · 7. Premier administrateur · 8. Structure

## 1. Fonctionnalités

- **Comptes** : inscription, connexion, déconnexion, mot de passe oublié (Supabase Auth, session persistée).
- **Profil** : photo, nom, statut, confidentialité (dernière connexion, statut en ligne, accusés de lecture).
- **Messagerie temps réel** : texte, réponses, transfert, suppression (pour moi / pour tous), réactions,
  statuts envoyé/lu, « en train d'écrire… », pagination de l'historique (Supabase Realtime).
- **Médias** : photos, vidéos, fichiers, messages vocaux, upload avec progression (Supabase Storage).
- **Groupes** : création, membres, rôles, « seuls les admins peuvent publier », quitter.
- **Statuts** : texte, photo, vidéo, expiration à 24 h (purge horaire par pg_cron).
- **Appels audio/vidéo** : WebRTC, signalisation via Supabase.
- **Administration** : statistiques, signalements, bannissement — via des fonctions RPC Postgres qui
  vérifient le rôle admin côté serveur.
- **Sécurité** : Row Level Security sur toutes les tables, privilèges de colonnes (un utilisateur ne peut
  pas s'auto-promouvoir admin), politiques Storage par dossier.

## 2. Limitations

- **Notifications : uniquement app ouverte ou en arrière-plan récent.** Supabase n'a pas de service
  de push. Les notifications locales (`service/MessageNotifier.kt`) sont alimentées par Realtime.
  Pour être notifié app fermée, il faut ajouter un push externe (FCM, UnifiedPush, ntfy…) déclenché
  par un Database Webhook / Edge Function. Les appels entrants ne sonnent donc aussi que app ouverte.
- **Pas de file d'écriture hors-ligne** (Firestore en avait une) : un message envoyé sans réseau
  échoue et l'utilisateur doit réessayer.
- **Pas de chiffrement de bout en bout** (TLS en transit, chiffrement au repos par Supabase).
- **Appels** : un serveur TURN est nécessaire pour être fiable (section 4).
- **Médias** : bucket public `media` (URL non devinables, UUID). Plan gratuit : 50 Mo max par fichier.
- **Connexion par e-mail** uniquement (le téléphone n'est qu'un champ de profil recherchable).
- Les fichiers médias des statuts expirés restent dans le bucket (seules les lignes sont purgées).
- Compilation non vérifiée (voir avertissement en haut).

## 3. Configurer Supabase

1. Crée un projet sur [supabase.com](https://supabase.com). Note le **mot de passe de la base**.
2. **Project Settings → API** : copie l'**URL** et la clé **anon public** (jamais la `service_role`).
3. **Authentication → Providers → Email** : pour tester sans mail de confirmation, désactive
   *Confirm email*. (Sinon, l'utilisateur doit cliquer le lien reçu avant de se connecter.)
4. Applique le schéma : soit via GitHub Actions (section 6, workflow *Déployer la base Supabase*),
   soit en collant `supabase/migrations/20261002000000_afrchat_schema.sql` dans **SQL Editor → Run**.
5. **Database → Extensions** : active `pg_cron` si tu veux la purge automatique des statuts expirés.

## 5. Configurer les appels audio/vidéo (WebRTC + TURN)

Les appels utilisent Supabase (Postgres + Realtime) comme canal de signalisation (aucun serveur à héberger pour ça),
mais un **serveur TURN** est nécessaire pour que l'appel s'établisse de façon fiable entre deux
réseaux différents (ex. un appelant en 4G et un appelé derrière une box Wi-Fi/NAT).

Deux options :

**A. Service TURN managé (le plus simple)** — Metered.ca, Twilio Network Traversal Service,
Xirsys, etc. proposent un plan gratuit ou peu coûteux. Une fois inscrit, tu obtiens une URL, un
nom d'utilisateur et un mot de passe TURN.

**B. Auto-héberger [coturn](https://github.com/coturn/coturn)** sur un petit VPS (ex. 2€/mois).

Dans les deux cas, renseigne les identifiants dans :
```kotlin
// app/src/main/java/com/afrchat/app/utils/Constants.kt
const val TURN_URL = "turn:ton-serveur-turn:3478"
const val TURN_USERNAME = "..."
const val TURN_CREDENTIAL = "..."
```
Sans cette configuration, l'app utilise uniquement des serveurs STUN publics (Google) : les
appels fonctionneront dans certains cas mais pas tous.

## 6. Ouvrir et lancer le projet

1. Ouvre le dossier `AFR-CHAT/` (celui qui contient `settings.gradle.kts`) avec **Android
   Studio → Open**.
2. Android Studio propose automatiquement de générer le wrapper Gradle (`gradlew`) s'il est
   absent — accepte. Si besoin manuellement : `gradle wrapper --gradle-version 8.7` (nécessite
   Gradle installé une première fois, ou passe par Android Studio qui l'intègre déjà).
3. Laisse Android Studio synchroniser le projet (télécharge les dépendances — nécessite une
   connexion internet, contrairement à l'environnement où ce projet a été généré).
4. Renseigne `SUPABASE_URL` et `SUPABASE_ANON_KEY` dans `~/.gradle/gradle.properties` (ou en variables d'environnement).
5. Sélectionne un appareil/émulateur (API 24 minimum, pour les appels)
   et clique sur **Run ▶**.

## 7. Générer l'APK

**APK debug (installable pour les tests immédiatement) :**
```bash
./gradlew assembleDebug
```
L'APK se trouve dans `app/build/outputs/apk/debug/AFR-CHAT-debug.apk` (le nom de sortie est déjà
configuré via `archivesName` dans `app/build.gradle.kts`). Installable directement
(`adb install AFR-CHAT-debug.apk`) sans configuration de signature supplémentaire.

**APK release (signé, pour distribution) :**
1. Génère un keystore si tu n'en as pas :
   ```bash
   keytool -genkey -v -keystore afrchat-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias afrchat
   ```
2. Renseigne les variables d'environnement (ou `~/.gradle/gradle.properties`) :
   ```
   AFRCHAT_STORE_FILE=/chemin/absolu/vers/afrchat-release.jks
   AFRCHAT_STORE_PASSWORD=...
   AFRCHAT_KEY_ALIAS=afrchat
   AFRCHAT_KEY_PASSWORD=...
   ```
3. Génère l'APK :
   ```bash
   ./gradlew assembleRelease
   ```
   Résultat : `app/build/outputs/apk/release/AFR-CHAT-release.apk`.
4. (Optionnel, recommandé pour le Play Store) Génère plutôt un **App Bundle** :
   `./gradlew bundleRelease` → `app/build/outputs/bundle/release/AFR-CHAT-release.aab`.

## 6. GitHub Actions (compilation APK + déploiement base)

Deux workflows dans `.github/workflows/` :

- **`build-apk.yml`** — compile l'APK à chaque push sur `main`, en manuel, ou sur un tag `v*`.
- **`supabase-deploy.yml`** — applique `supabase/migrations/` sur ton projet (manuel, ou automatique
  quand une migration change sur `main`).

Secrets à créer (Settings → Secrets and variables → Actions) :

| Secret | Pour | Contenu |
|---|---|---|
| `SUPABASE_URL`, `SUPABASE_ANON_KEY` | APK | URL du projet + clé anon (Project Settings → API) |
| `SUPABASE_ACCESS_TOKEN`, `SUPABASE_PROJECT_REF`, `SUPABASE_DB_PASSWORD` | déploiement base | jeton personnel, identifiant du projet, mot de passe de la base |
| `AFRCHAT_TURN_URL`, `_USERNAME`, `_CREDENTIAL` | APK (optionnel) | identifiants TURN pour les appels |
| `AFRCHAT_KEYSTORE_BASE64`, `AFRCHAT_STORE_PASSWORD`, `AFRCHAT_KEY_ALIAS`, `AFRCHAT_KEY_PASSWORD` | APK release signé (optionnel, les 4 ensemble) | `base64 -w0 afrchat.jks` + mots de passe |

Récupérer l'APK : onglet **Actions → run → Artifacts**, ou (conseillé sur téléphone) crée un tag :
`git tag v1.0.0 && git push origin v1.0.0` → l'APK est dans les **Releases** du dépôt.
Sans `SUPABASE_URL`/`SUPABASE_ANON_KEY`, le build compile mais l'artifact s'appelle
`AFR-CHAT-debug-SANS-SUPABASE` et l'APK ne peut pas se connecter. Le `versionCode` suit le numéro de run.

## 7. Premier administrateur

Inscris-toi dans l'app, puis exécute dans **Supabase → SQL Editor** (en remplaçant l'e-mail) le contenu
de `supabase/bootstrap-admin.sql` :

```sql
update public.profiles set is_admin = true
 where id = (select id from auth.users where email = 'TON_EMAIL@exemple.com');
```
Reconnecte-toi : le bouton « Espace administrateur » apparaît sur l'écran de profil.

## 8. Structure

```
AFR-CHAT/
├── app/src/main/java/com/afrchat/app/
│   ├── data/model/        Modèles de l'app
│   ├── data/remote/       DTO Postgres (snake_case) + pont Realtime
│   ├── data/repository/   Accès Supabase (Auth, PostgREST, RPC, Storage)
│   ├── service/           Notifications locales, appel en premier plan, resynchronisation
│   ├── ui/ · navigation/ · di/ · utils/
├── supabase/
│   ├── migrations/        Schéma, RLS, RPC, Realtime, Storage, pg_cron
│   ├── bootstrap-admin.sql
│   └── config.toml
├── .github/workflows/     build-apk.yml · supabase-deploy.yml
├── ARCHITECTURE.md
└── README.md
```
