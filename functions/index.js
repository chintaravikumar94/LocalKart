/**
 * LocalKart Cloud Functions — send FCM push to the seller when a customer
 * creates an order, booking, or service request.
 *
 * Deploy: see docs/PUSH_SETUP.md (requires Blaze plan + Firebase CLI).
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/** Look up the FCM token of the seller who owns a store/service doc. */
async function ownerToken(collection, docId) {
  if (!docId) return null;
  const doc = await db.collection(collection).doc(docId).get();
  const ownerUid = doc.exists ? doc.data().ownerUid : null;
  if (!ownerUid) return null;
  const user = await db.collection("users").doc(ownerUid).get();
  return user.exists ? (user.data().fcmToken || null) : null;
}

async function send(token, title, body) {
  if (!token) return;
  try {
    await admin.messaging().send({ token, notification: { title, body } });
  } catch (e) {
    console.error("FCM send failed:", e.message);
  }
}

exports.onNewOrder = functions.firestore
  .document("orders/{id}")
  .onCreate(async (snap) => {
    const o = snap.data();
    const token = await ownerToken("stores", o.storeId);
    const count = (o.items || []).length;
    await send(token, "New order received", `₹${o.total} · ${count} item(s)`);
  });

exports.onNewBooking = functions.firestore
  .document("bookings/{id}")
  .onCreate(async (snap) => {
    const b = snap.data();
    const token = await ownerToken("services", b.providerId);
    await send(token, "New booking", b.service || "Service booking requested");
  });

exports.onNewServiceRequest = functions.firestore
  .document("serviceRequests/{id}")
  .onCreate(async (snap) => {
    const r = snap.data();
    const token = await ownerToken("services", r.providerId);
    await send(token, "New job request", r.title || "Service requested");
  });
