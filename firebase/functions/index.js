/**
 * Cloud Functions — AFR CHAT
 *
 * Déploiement : `firebase deploy --only functions` depuis le dossier firebase/ (voir README).
 * Région utilisée côté client (FirebaseFunctions.getInstance("europe-west1")) : adapte-la ici
 * et côté client (di/AppModule.kt) si tu déploies ailleurs.
 */

const { onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 10 });

const db = admin.firestore();
const messaging = admin.messaging();
const storage = admin.storage();

// ============================================================================
// RECHERCHE : maintient un champ "firstNameLower" (et lastNameLower) à chaque
// écriture d'un profil utilisateur, pour permettre les requêtes de préfixe
// insensibles à la casse utilisées par UserRepository.searchUsers().
// ============================================================================
exports.onUserWrite = onDocumentWritten("users/{userId}", async (event) => {
  const after = event.data.after;
  if (!after.exists) return null;

  const data = after.data();
  const firstNameLower = (data.firstName || "").toLowerCase();
  const lastNameLower = (data.lastName || "").toLowerCase();

  if (data.firstNameLower === firstNameLower && data.lastNameLower === lastNameLower) {
    return null; // évite une boucle d'écriture infinie
  }
  return after.ref.update({ firstNameLower, lastNameLower });
});

// ============================================================================
// NOTIFICATIONS : nouveau message -> notification push data-only pour chaque
// participant sauf l'expéditeur. Le contenu du message n'est jamais inclus
// dans la charge utile FCM (voir NotificationRepository.kt côté client).
// ============================================================================
exports.onNewMessageCreated = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const message = event.data.data();
    const conversationId = event.params.conversationId;

    const convoSnap = await db.collection("conversations").doc(conversationId).get();
    if (!convoSnap.exists) return null;
    const convo = convoSnap.data();

    const recipients = (convo.participantIds || []).filter((uid) => uid !== message.senderId);
    if (recipients.length === 0) return null;

    const senderSnap = await db.collection("users").doc(message.senderId).get();
    const senderName = senderSnap.exists ? `${senderSnap.data().firstName} ${senderSnap.data().lastName}`.trim() : "AFR CHAT";

    const tokens = [];
    for (const uid of recipients) {
      const userSnap = await db.collection("users").doc(uid).get();
      if (userSnap.exists) tokens.push(...(userSnap.data().fcmTokens || []));
    }
    if (tokens.length === 0) return null;

    const payload = {
      data: {
        type: "new_message",
        conversationId,
        senderName,
        isGroup: String(convo.type === "group"),
      },
      tokens,
      android: { priority: "high" },
    };

    const response = await messaging.sendEachForMulticast(payload);
    await cleanupInvalidTokens(response, tokens, recipients);
    await incrementDailyMessageCounter();
    return null;
  }
);

// Incrémente un compteur journalier (collection "stats_daily_messages/{YYYY-MM-DD}") à chaque
// message envoyé, pour permettre à getAdminStats de calculer "messages sur 30 jours" avec au
// maximum 30 lectures de documents, au lieu de scanner toutes les sous-collections de messages.
async function incrementDailyMessageCounter() {
  const today = new Date().toISOString().slice(0, 10); // "YYYY-MM-DD"
  await db.collection("stats_daily_messages").doc(today).set(
    { count: admin.firestore.FieldValue.increment(1) },
    { merge: true }
  );
}

// ============================================================================
// APPELS : nouvel appel entrant -> notification push (avec full-screen intent
// géré côté client) pour que le callee voie l'écran d'appel même app fermée.
// ============================================================================
exports.onIncomingCall = onDocumentCreated("calls/{callId}", async (event) => {
  const call = event.data.data();
  if (call.status !== "ringing") return null;

  const [callerSnap, calleeSnap] = await Promise.all([
    db.collection("users").doc(call.callerUid).get(),
    db.collection("users").doc(call.calleeUid).get(),
  ]);
  if (!calleeSnap.exists) return null;

  const callerName = callerSnap.exists ? `${callerSnap.data().firstName} ${callerSnap.data().lastName}`.trim() : "AFR CHAT";
  const tokens = calleeSnap.data().fcmTokens || [];
  if (tokens.length === 0) return null;

  const payload = {
    data: {
      type: "incoming_call",
      callId: event.params.callId,
      callerName,
      isVideo: String(!!call.isVideo),
    },
    tokens,
    android: { priority: "high" },
  };

  const response = await messaging.sendEachForMulticast(payload);
  await cleanupInvalidTokens(response, tokens, [call.calleeUid]);
  return null;
});

// ============================================================================
// STATUTS : suppression planifiée des statuts expirés (toutes les heures),
// y compris leur fichier média associé dans Storage.
// ============================================================================
exports.cleanupExpiredStories = onSchedule("every 60 minutes", async () => {
  const now = Date.now();
  const expired = await db.collection("stories").where("expiresAt", "<=", now).get();
  if (expired.empty) return null;

  const batch = db.batch();
  for (const doc of expired.docs) {
    const data = doc.data();
    batch.delete(doc.ref);
    if (data.mediaUrl) {
      try {
        const path = decodeURIComponent(new URL(data.mediaUrl).pathname.split("/o/")[1].split("?")[0]);
        await storage.bucket().file(path).delete({ ignoreNotFound: true });
      } catch (e) {
        console.warn("Impossible de supprimer le média du statut", doc.id, e.message);
      }
    }
  }
  await batch.commit();
  console.log(`${expired.size} statut(s) expiré(s) supprimé(s).`);
  return null;
});

// ============================================================================
// ADMINISTRATION — toutes ces fonctions vérifient explicitement le custom
// claim "admin" côté serveur avant d'agir. Le client ne peut jamais réaliser
// ces actions directement (voir firestore.rules).
// ============================================================================
function assertIsAdmin(request) {
  if (!request.auth || request.auth.token.admin !== true) {
    throw new HttpsError("permission-denied", "Action réservée aux administrateurs AFR CHAT.");
  }
}

exports.banUser = onCall(async (request) => {
  assertIsAdmin(request);
  const { targetUid, reason } = request.data;
  if (!targetUid) throw new HttpsError("invalid-argument", "targetUid manquant.");

  await db.collection("users").doc(targetUid).update({ isBanned: true, banReason: reason || "" });
  await admin.auth().updateUser(targetUid, { disabled: true });
  return { success: true };
});

exports.unbanUser = onCall(async (request) => {
  assertIsAdmin(request);
  const { targetUid } = request.data;
  if (!targetUid) throw new HttpsError("invalid-argument", "targetUid manquant.");

  await db.collection("users").doc(targetUid).update({ isBanned: false, banReason: "" });
  await admin.auth().updateUser(targetUid, { disabled: false });
  return { success: true };
});

exports.resolveReport = onCall(async (request) => {
  assertIsAdmin(request);
  const { reportId, status } = request.data;
  if (!reportId || !["reviewed", "dismissed"].includes(status)) {
    throw new HttpsError("invalid-argument", "Paramètres invalides.");
  }
  await db.collection("reports").doc(reportId).update({ status });
  return { success: true };
});

/**
 * Accorde ou retire le rôle admin. Pour créer le tout premier administrateur (avant qu'aucun
 * compte n'ait le claim "admin"), utilise le script scripts/bootstrap-admin.js décrit dans le
 * README — cette fonction callable ne peut pas être utilisée pour le tout premier compte.
 */
exports.setAdminRole = onCall(async (request) => {
  assertIsAdmin(request);
  const { targetUid, grant } = request.data;
  if (!targetUid || typeof grant !== "boolean") {
    throw new HttpsError("invalid-argument", "Paramètres invalides.");
  }
  await admin.auth().setCustomUserClaims(targetUid, { admin: grant });
  await db.collection("users").doc(targetUid).update({ isAdmin: grant });
  return { success: true };
});

exports.getAdminStats = onCall(async (request) => {
  assertIsAdmin(request);

  const totalUsersSnap = await db.collection("users").count().get();
  const activeGroupsSnap = await db.collection("groups").count().get();
  const openReportsSnap = await db.collection("reports").where("status", "==", "open").count().get();
  const messagesLast30Days = await sumLast30DaysMessages();

  return {
    totalUsers: totalUsersSnap.data().count,
    activeGroups: activeGroupsSnap.data().count,
    openReports: openReportsSnap.data().count,
    messagesLast30Days,
  };
});

/**
 * Additionne le compteur journalier "stats_daily_messages/{YYYY-MM-DD}" sur les 30 derniers
 * jours (au maximum 30 lectures de documents — bien moins coûteux qu'un scan de toutes les
 * sous-collections de messages de toutes les conversations).
 */
async function sumLast30DaysMessages() {
  const days = [];
  for (let i = 0; i < 30; i++) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    days.push(d.toISOString().slice(0, 10));
  }
  const docs = await Promise.all(days.map((day) => db.collection("stats_daily_messages").doc(day).get()));
  return docs.reduce((sum, doc) => sum + (doc.exists ? doc.data().count || 0 : 0), 0);
}

// ============================================================================
// Utilitaire : retire les jetons FCM invalides/expirés des profils utilisateurs.
// ============================================================================
async function cleanupInvalidTokens(response, tokens, uids) {
  const invalidTokens = [];
  response.responses.forEach((res, idx) => {
    if (!res.success && ["messaging/invalid-registration-token", "messaging/registration-token-not-registered"].includes(res.error?.code)) {
      invalidTokens.push(tokens[idx]);
    }
  });
  if (invalidTokens.length === 0) return;

  for (const uid of uids) {
    await db.collection("users").doc(uid).update({
      fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalidTokens),
    }).catch(() => {});
  }
}
