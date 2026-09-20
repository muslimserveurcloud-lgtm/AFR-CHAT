/**
 * Script à exécuter UNE SEULE FOIS, localement, pour créer le tout premier administrateur
 * AFR CHAT (avant qu'aucun compte n'ait le custom claim "admin", la Cloud Function setAdminRole
 * ne peut être appelée par personne).
 *
 * Utilisation :
 *   1. Télécharge une clé de compte de service depuis la console Firebase
 *      (Paramètres du projet > Comptes de service > Générer une nouvelle clé privée)
 *      et enregistre-la ici sous le nom "serviceAccountKey.json" (NE JAMAIS committer ce fichier).
 *   2. npm install firebase-admin (dans ce dossier scripts/, ou réutilise functions/node_modules)
 *   3. node bootstrap-admin.js <uid-de-l-utilisateur-a-promouvoir>
 */
const admin = require("firebase-admin");
const serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });

const targetUid = process.argv[2];
if (!targetUid) {
  console.error("Usage : node bootstrap-admin.js <uid>");
  process.exit(1);
}

(async () => {
  await admin.auth().setCustomUserClaims(targetUid, { admin: true });
  await admin.firestore().collection("users").doc(targetUid).update({ isAdmin: true });
  console.log(`✅ ${targetUid} est maintenant administrateur AFR CHAT.`);
  process.exit(0);
})().catch((e) => {
  console.error("Échec :", e);
  process.exit(1);
});
