# AFR CHAT

Application Android native de messagerie instantanée — Kotlin + Jetpack Compose + Firebase +
WebRTC. Architecture MVVM / Repository, identité visuelle originale (voir `app/src/main/res`).

> ⚠️ **À lire avant tout** : ce projet a été généré dans un environnement sans accès réseau
> (pas de téléchargement possible du SDK Android, de Gradle, ni des dépendances). **Le code
> n'a donc pas pu être compilé ni exécuté ici**, et aucun APK n'a été généré automatiquement.
> Tout le code est réel et complet (aucun pseudo-code, aucune fonction laissée en "à faire"),
> mais tu dois toi-même l'ouvrir dans Android Studio pour compiler, corriger d'éventuelles
> erreurs mineures liées à l'environnement (versions de SDK installées, etc.) et générer l'APK.
> La section [7. Générer l'APK](#7-générer-lapk) t'explique comment faire, en 10-15 minutes.

## Sommaire
1. [Ce qui est implémenté](#1-ce-qui-est-implémenté)
2. [Limitations connues](#2-limitations-connues)
3. [Prérequis](#3-prérequis)
4. [Configurer Firebase](#4-configurer-firebase)
5. [Configurer les appels audio/vidéo (WebRTC + TURN)](#5-configurer-les-appels-audiovidéo-webrtc--turn)
6. [Ouvrir et lancer le projet](#6-ouvrir-et-lancer-le-projet)
7. [Générer l'APK](#7-générer-lapk)
8. [Créer le premier administrateur](#8-créer-le-premier-administrateur)
9. [Checklist de vérification manuelle](#9-checklist-de-vérification-manuelle)
10. [Structure du projet](#10-structure-du-projet)

---

## 1. Ce qui est implémenté

Code Kotlin complet (pas d'extraits, pas de stubs) pour :

- **Comptes** : inscription (prénom, nom, e-mail, mot de passe, validations), connexion,
  déconnexion, mot de passe oublié, maintien de session (natif Firebase Auth).
- **Profil** : photo, nom, statut, paramètres de confidentialité (dernière connexion, statut en
  ligne, accusés de lecture).
- **Messagerie privée temps réel** : texte, réponses, transfert, suppression (pour moi / pour
  tous), réactions emoji, statuts envoyé/distribué/lu, indicateur "en train d'écrire…", scroll +
  pagination de l'historique.
- **Médias** : photos, vidéos, fichiers, messages vocaux (enregistrement natif MediaRecorder),
  upload avec barre de progression vers Firebase Storage.
- **Groupes** : création, ajout/retrait de membres, rôles admin/membre, permissions
  ("seuls les admins peuvent publier"), quitter le groupe.
- **Statuts/Stories** : texte, photo, vidéo, expiration automatique à 24h (Cloud Function
  planifiée qui nettoie aussi les fichiers Storage associés).
- **Notifications push** (FCM) : nouveaux messages et appels entrants, contenu du message jamais
  exposé sur l'écran verrouillé.
- **Recherche** : utilisateurs (par prénom/nom), avec navigation directe vers la conversation.
- **Appels audio/vidéo** : implémentation WebRTC réelle (négociation SDP, échange ICE via
  Firestore, flux média pair-à-pair, coupe micro/caméra, changement de caméra) — voir limitations
  ci-dessous concernant l'infrastructure TURN à fournir.
- **Mode hors-ligne** : persistance locale Firestore, écritures rejouées automatiquement à la
  reconnexion, WorkManager pour la resynchronisation du statut/jeton FCM.
- **Espace administrateur** : tableau de bord (statistiques, dont un compteur réel de messages
  sur 30 jours via un compteur journalier incrémental), gestion des signalements,
  bannissement/débannissement — toute la logique sensible est côté serveur (Cloud Functions +
  custom claims), jamais côté client.
- **Sécurité** : règles Firestore/Storage complètes (`firebase/firestore.rules`,
  `firebase/storage.rules`), App Check (Play Integrity), validation des champs côté client.
- **Mode clair/sombre**, identité visuelle originale (nom, logo, palette — voir ci-dessous).

## 2. Limitations connues

Sois transparent avec toi-même sur ces points avant de considérer le projet "terminé" :

- **Compilation non vérifiée ici.** Le code suit scrupuleusement les API Kotlin/Compose/Firebase/
  WebRTC actuelles, mais n'ayant pas pu exécuter Gradle, il est possible qu'Android Studio
  signale une ou deux erreurs mineures (import manquant, léger désaccord de version entre
  bibliothèques) à corriger — l'auto-complétion et le "Quick Fix" d'Android Studio suffiront dans
  l'immense majorité des cas.
- **Chiffrement de bout en bout : non implémenté.** Les données sont chiffrées en transit (TLS)
  et au repos (Firebase), mais pas chiffrées de bout en bout. Ne communique jamais que l'app
  offre un chiffrement de bout en bout tant que ce n'est pas explicitement ajouté (bibliothèque
  éprouvée comme libsignal-client, effort de développement significatif à part entière).
- **Appels WebRTC : nécessitent un serveur TURN pour être fiables en conditions réelles**
  (réseaux mobiles/Wi-Fi grand public avec NAT/pare-feu). Sans TURN configuré, les appels ne
  fonctionneront que dans certaines conditions réseau favorables. Voir section 5.
- **Recherche par numéro de téléphone** : implémentée (recherche par préfixe sur le champ
  `phone`, en plus du nom). Le champ téléphone est facultatif à l'inscription, conformément à
  la demande "numéro de téléphone OU adresse e-mail" — l'e-mail reste l'identifiant de connexion
  Firebase Auth dans cette version ; ajouter la connexion par téléphone nécessiterait d'activer
  la méthode "Téléphone" dans Firebase Authentication (vérification SMS) en plus de l'e-mail.
- **Lecture vidéo dans les bulles de chat et les statuts** : lecteur ExoPlayer/Media3 intégré
  (`ui/components/VideoPlayerView.kt`), branché dans les bulles de message et le visualiseur de
  statuts.
- Aucun APK n'a été généré automatiquement (voir avertissement en haut de ce fichier).
- **`gradlew` / `gradlew.bat` sont inclus**, mais le binaire `gradle/wrapper/gradle-wrapper.jar`
  ne l'est pas (fichier binaire, impossible à générer sans accès réseau ici). Android Studio le
  télécharge automatiquement à l'ouverture du projet ; en ligne de commande sans Android Studio,
  lance d'abord `gradle wrapper --gradle-version 8.7` avec une installation locale de Gradle.

## 3. Prérequis

- [Android Studio](https://developer.android.com/studio) (Koala ou plus récent recommandé)
- JDK 17 (fourni avec Android Studio)
- Un compte Google + un projet [Firebase](https://console.firebase.google.com)
- Un appareil Android (API 24+) ou un émulateur avec Google Play Services

## 4. Configurer Firebase

1. Crée un projet sur [console.firebase.google.com](https://console.firebase.google.com).
2. Ajoute une application Android avec le nom de package **`com.afrchat.app`**.
3. Télécharge le fichier **`google-services.json`** généré et place-le dans
   `app/google-services.json` (remplace le gabarit `app/google-services.json.example`, qui n'est
   qu'un exemple de structure).
4. Dans la console Firebase, active :
   - **Authentication** → méthode de connexion **E-mail/Mot de passe**.
   - **Firestore Database** → crée la base (mode production).
   - **Storage** → crée le bucket par défaut.
   - **Cloud Messaging** → rien à faire manuellement, fonctionne dès que `google-services.json`
     est en place.
   - **App Check** → enregistre l'app avec le fournisseur **Play Integrity**.
5. Déploie les règles de sécurité et les Cloud Functions (nécessite le
   [Firebase CLI](https://firebase.google.com/docs/cli) : `npm install -g firebase-tools`) :
   ```bash
   cd firebase
   firebase login
   firebase use --add          # sélectionne ton projet, ou édite .firebaserc directement
   firebase deploy --only firestore:rules,firestore:indexes,storage:rules
   cd functions && npm install && cd ..
   firebase deploy --only functions
   ```
6. Le plan **Blaze** (paiement à l'usage) est requis pour déployer des Cloud Functions et pour
   les appels sortants réseau depuis les fonctions (envoi de notifications). Le plan Blaze inclut
   un quota gratuit généreux, largement suffisant pour le développement/test.

## 5. Configurer les appels audio/vidéo (WebRTC + TURN)

Les appels utilisent Firestore comme canal de signalisation (aucun serveur à héberger pour ça),
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
4. Vérifie que `app/google-services.json` est bien présent (étape 4.3).
5. Sélectionne un appareil/émulateur (API 24 minimum, Google Play Services requis pour FCM/appels)
   et clique sur **Run ▶**.

## 7. Générer l'APK

**APK debug (installable pour les tests immédiatement) :**
```bash
./gradlew assembleDebug
```
L'APK se trouve dans `app/build/outputs/apk/debug/app-debug.apk`. Installable directement
(`adb install app-debug.apk`) sans configuration de signature supplémentaire.

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
   Résultat : `app/build/outputs/apk/release/app-release.apk` (nommé `AFR-CHAT.apk` une fois
   renommé/exporté — Gradle ne permet pas nativement de renommer l'artefact, ajoute
   `archivesName.set("AFR-CHAT")` dans le bloc `android {}` de `app/build.gradle.kts` si tu veux
   ce nom exact en sortie).
4. (Optionnel, recommandé pour le Play Store) Génère plutôt un **App Bundle** :
   `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.

## 8. Créer le premier administrateur

Aucun compte n'a le rôle admin au départ (et pour cause : la fonction qui attribue ce rôle
nécessite déjà d'être admin). Utilise le script fourni **une seule fois** :

```bash
cd firebase/scripts
# place ta clé de compte de service ici sous le nom serviceAccountKey.json
# (Console Firebase → Paramètres du projet → Comptes de service → Générer une nouvelle clé privée)
npm install firebase-admin
node bootstrap-admin.js <UID_DE_TON_COMPTE>
```
Le UID se trouve dans Firebase Authentication → l'onglet Users, ou dans Firestore
`users/{uid}`. Une fois ce script exécuté, reconnecte-toi dans l'app : le bouton "Espace
administrateur" apparaît sur l'écran de profil.

## 9. Checklist de vérification manuelle

Cette checklist correspond point par point à la demande initiale. Comme le projet n'a pas pu
être compilé dans cet environnement, **coche-la toi-même une fois le build lancé** — chaque point
correspond à du code réellement écrit, pas à une promesse :

- [ ] Le projet compile sans erreur (`./gradlew assembleDebug`)
- [ ] L'application démarre sur un appareil/émulateur
- [ ] Inscription d'un nouveau compte fonctionne
- [ ] Connexion / déconnexion fonctionnent
- [ ] Envoi d'un message texte entre deux comptes fonctionne
- [ ] Réception en temps réel (sans recharger l'app) fonctionne
- [ ] Une notification apparaît pour un nouveau message (app en arrière-plan)
- [ ] Création d'un groupe et envoi d'un message de groupe fonctionnent
- [ ] Envoi d'une image fonctionne (aperçu + upload + réception)
- [ ] Un appel audio/vidéo s'établit entre deux appareils (avec TURN configuré si réseaux
      différents)
- [ ] Un statut publié apparaît chez les contacts et disparaît après 24h

## 10. Structure du projet

```
AFR-CHAT/
├── app/                         Application Android
│   └── src/main/
│       ├── java/com/afrchat/app/
│       │   ├── data/            Modèles + Repositories (accès Firebase)
│       │   ├── di/               Injection de dépendances (Hilt)
│       │   ├── navigation/       Graphe de navigation Compose
│       │   ├── service/          FCM, appel en premier plan, hors-ligne
│       │   ├── ui/               Écrans Compose + ViewModels, par fonctionnalité
│       │   └── utils/            Constantes, validateurs, formatage
│       └── res/                  Ressources (couleurs, chaînes, icônes)
├── firebase/
│   ├── firestore.rules           Règles de sécurité Firestore
│   ├── storage.rules             Règles de sécurité Storage
│   ├── firestore.indexes.json    Index composites requis
│   ├── functions/                Cloud Functions (notifications, admin, nettoyage statuts)
│   └── scripts/bootstrap-admin.js
├── ARCHITECTURE.md               Détail de l'architecture et du schéma de données
└── README.md                     Ce fichier
```

---

**Identité visuelle** — Nom : *AFR CHAT*. Palette originale : violet-indigo `#5B4FE9` (primaire),
corail `#FF7A59` (accent), avec variante sombre dédiée. Logo original (bulle de discussion +
étincelle de connexion) en `app/src/main/res/drawable/ic_launcher_foreground.xml`, sans lien
avec l'identité visuelle de WhatsApp ou de toute autre application tierce.
