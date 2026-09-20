# Architecture AFR CHAT

## 1. Vue d'ensemble

```
Android (Kotlin, Jetpack Compose, MVVM)
        │
        ├── Firebase Authentication  → comptes utilisateurs (e-mail / mot de passe)
        ├── Cloud Firestore          → données temps réel (conversations, messages, groupes…)
        ├── Firebase Storage         → médias (images, vidéos, fichiers, vocaux, photos)
        ├── Firebase Cloud Messaging → notifications push
        ├── Cloud Functions          → logique serveur sensible (modération, notifications,
        │                              nettoyage des statuts expirés)
        └── WebRTC (pair-à-pair)     → flux audio/vidéo des appels, signalisation via Firestore
```

Couche par couche (MVVM + Repository) :

```
ui/{feature}/*Screen.kt        → Composables (affichage uniquement)
ui/{feature}/*ViewModel.kt     → état UI + logique de présentation (Hilt ViewModel)
data/repository/*Repository.kt → accès aux données, une seule source de vérité par domaine
data/model/*.kt                → modèles de données (Firestore-serializable data classes)
di/AppModule.kt                 → fourniture des instances Firebase (Hilt)
```

Chaque écran observe un `StateFlow` exposé par son ViewModel ; le ViewModel ne connaît jamais
Firebase directement, il passe toujours par un repository — ce qui permet de remplacer Firebase
par un autre backend plus tard sans toucher à l'UI.

## 2. Structure de la base de données (Cloud Firestore)

```
users/{uid}
  firstName, lastName, email, phone, photoUrl, statusMessage,
  isOnline, lastSeen, fcmTokens[], privacy{}, isAdmin, isBanned,
  firstNameLower, lastNameLower   (maintenus par la Cloud Function onUserWrite, pour la recherche)
  └── notifications/{id}          (optionnel, accusés de notification)

conversations/{id}
  type: "private" | "group", participantIds[], groupId?,
  lastMessage, lastMessageType, lastMessageSenderId, lastMessageAt,
  unreadCount{uid: n}, typingUserIds[], isArchived{uid: bool}, isMuted{uid: bool}
  └── messages/{id}
        senderId, type, text, mediaUrl, fileName, replyToMessageId,
        reactions{uid: emoji}, status, deletedFor[], isDeletedForEveryone, sentAt

groups/{id}
  name, description, photoUrl, ownerUid, adminUids[], memberUids[],
  onlyAdminsCanPost, onlyAdminsCanEditInfo, conversationId

stories/{id}
  ownerUid, type, content, mediaUrl, backgroundColor,
  viewerUids[], createdAt, expiresAt   (nettoyage auto par Cloud Function planifiée)

calls/{id}
  callerUid, calleeUid, isVideo, status, offerSdp, answerSdp, createdAt, endedAt
  ├── callerCandidates/{id}   (candidats ICE du demandeur)
  └── calleeCandidates/{id}   (candidats ICE du destinataire)

reports/{id}
  reporterUid, targetType, targetId, reason, details, status, createdAt
```

### Pourquoi cette structure ?
- **`conversations` séparée de `messages`** (sous-collection) : permet de lister rapidement les
  discussions (un seul document léger par conversation) sans charger tout l'historique, et de
  paginer les messages indépendamment.
- **`unreadCount` par uid dans le document conversation** : évite de compter les messages non lus
  à chaque affichage (coûteux), le compteur est incrémenté/désincrémenté directement.
- **`groups` séparé de `conversations`** : un groupe a un cycle de vie propre (membres, rôles,
  paramètres) indépendant de la discussion elle-même ; la conversation de groupe référence son
  `groupId`.
- **`calls` avec sous-collections de candidats ICE séparées par rôle** : évite les conflits
  d'écriture concurrente entre l'appelant et l'appelé pendant la négociation WebRTC.

## 3. Mode hors-ligne

La persistance locale Firestore est activée (`AfrChatApplication.onCreate`) : les lectures sont
servies depuis le cache local quand le réseau est indisponible, et les écritures (nouveaux
messages, réactions, etc.) sont mises en file d'attente localement puis rejouées automatiquement
à la reconnexion — sans code supplémentaire à écrire pour chaque fonctionnalité. `ConnectivityObserver`
expose l'état réseau pour afficher un bandeau "hors ligne" dans l'UI si souhaité, et
`OfflineMessageSyncWorker` (WorkManager) republie le statut "en ligne" et rafraîchit le jeton FCM
après une coupure prolongée.

## 4. Sécurité

- **Authentification** : Firebase Authentication (e-mail/mot de passe), jeton rafraîchi
  automatiquement, session maintenue nativement.
- **Autorisations serveur** : `firebase/firestore.rules` et `firebase/storage.rules` empêchent
  tout accès aux données d'un autre utilisateur (voir commentaires dans ces fichiers).
- **Modération/admin** : jamais côté client. Toute action sensible (bannir, changer un rôle admin,
  traiter un signalement) passe par une Cloud Function *callable* qui vérifie le custom claim
  Firebase Auth `admin` côté serveur (`firebase/functions/index.js`).
- **App Check** (Play Integrity) : empêche des clients non authentifiés/modifiés d'appeler le
  backend Firebase.
- **Aucun secret dans le code source** : les identifiants Firebase viennent de
  `google-services.json` (fichier local, non committé) ; les identifiants TURN sont à renseigner
  dans `utils/Constants.kt` (voir README).
- **Chiffrement de bout en bout** : **non implémenté** dans cette version. Les données transitent
  en TLS entre l'app et Firebase (chiffrement en transit) et sont chiffrées au repos par Firebase,
  mais Firebase (donc l'opérateur du projet) peut techniquement lire le contenu des messages
  côté serveur. Ne prétends jamais à un chiffrement de bout en bout tant que cette fonctionnalité
  n'est pas ajoutée explicitement (voir section "Limitations" du README).

## 5. Appels audio/vidéo (WebRTC)

1. L'appelant crée un document `calls/{id}` avec son offre SDP.
2. Une Cloud Function (`onIncomingCall`) notifie le destinataire par push.
3. Le destinataire répond (accepte/refuse) : sa réponse SDP est écrite dans le même document.
4. Les candidats ICE de chaque côté sont échangés via les sous-collections
   `callerCandidates`/`calleeCandidates`.
5. Une fois la négociation terminée, l'audio/vidéo circule **directement entre les deux
   téléphones** (pair-à-pair) — Firestore n'est utilisé que pour la signalisation, jamais pour le
   flux média lui-même.

Un serveur TURN est nécessaire pour les cas où une connexion directe est impossible (la plupart
des réseaux mobiles/Wi-Fi grand public). Voir le README pour la configuration.
