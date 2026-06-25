/**
 * LocalKart Cloud Functions — send FCM push to the seller when a customer
 * creates an order, booking, or service request.
 *
 * Deploy: see docs/PUSH_SETUP.md (requires Blaze plan + Firebase CLI).
 */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

/* ---------- Razorpay config (set via `firebase functions:config:set` or env) ---------- */
function rzpCfg() {
  const c = (functions.config().razorpay) || {};
  return {
    keyId: c.key_id || process.env.RAZORPAY_KEY_ID || "",
    keySecret: c.key_secret || process.env.RAZORPAY_KEY_SECRET || "",
    webhookSecret: c.webhook_secret || process.env.RAZORPAY_WEBHOOK_SECRET || ""
  };
}

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

/* =================== Razorpay: activation + monthly subscription =================== */

/** The seller's first store/service → {name,type,category}. */
async function sellerInfo(uid) {
  const st = await db.collection("stores").where("ownerUid", "==", uid).limit(1).get();
  if (!st.empty) { const d = st.docs[0].data(); return { name: d.name || "", type: "store", category: d.category || "" }; }
  const sv = await db.collection("services").where("ownerUid", "==", uid).limit(1).get();
  if (!sv.empty) { const d = sv.docs[0].data(); return { name: d.name || "", type: "service", category: d.category || "" }; }
  return { name: "", type: "", category: "" };
}

/** Find the plan that applies to this seller's category + type. */
async function planFor(uid) {
  const info = await sellerInfo(uid);
  if (!info.category) return null;
  const snap = await db.collection("plans").get();
  const plans = snap.docs.map((d) => d.data());
  return plans.find((p) => p.category === info.category && (p.type === info.type || p.type === "both"))
      || plans.find((p) => p.category === info.category) || null;
}

/** Apply a verified payment: update billing/{uid} + add a payments record (same shape the admin uses). */
async function applyPayment(uid, kind, amountRupees) {
  const info = await sellerInfo(uid);
  const plan = await planFor(uid);
  const ref = db.collection("billing").doc(uid);
  if (kind === "activation") {
    await ref.set({
      name: info.name, type: info.type, category: info.category,
      activationPaid: true, activationAmount: amountRupees,
      activationAt: admin.firestore.FieldValue.serverTimestamp(),
      monthlyFee: (plan && plan.monthlyFee) || 0
    }, { merge: true });
  } else {
    const cur = (await ref.get()).data() || {};
    let base = (cur.nextDueAt && cur.nextDueAt.toMillis && cur.nextDueAt.toMillis() > Date.now())
      ? cur.nextDueAt.toDate() : new Date();
    base.setMonth(base.getMonth() + 1);
    await ref.set({
      name: info.name, type: info.type, category: info.category,
      subActive: true, monthlyFee: amountRupees,
      lastPaidAt: admin.firestore.FieldValue.serverTimestamp(),
      nextDueAt: admin.firestore.Timestamp.fromDate(base)
    }, { merge: true });
  }
  await db.collection("payments").add({
    uid, name: info.name, type: kind, amount: amountRupees,
    at: admin.firestore.FieldValue.serverTimestamp(), via: "razorpay"
  });
}

/** 1) Create a Razorpay order for activation or monthly. Amount is computed server-side from the plan. */
exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Please sign in.");
  const uid = context.auth.uid;
  const kind = data && data.kind;
  if (!["activation", "monthly"].includes(kind)) throw new functions.https.HttpsError("invalid-argument", "Invalid payment type.");
  const { keyId, keySecret } = rzpCfg();
  if (!keyId || !keySecret) throw new functions.https.HttpsError("failed-precondition", "Razorpay is not configured yet.");
  const plan = await planFor(uid);
  if (!plan) throw new functions.https.HttpsError("failed-precondition", "No plan set for your category yet. Contact admin.");
  const rupees = kind === "activation" ? (plan.activationFee || 0) : (plan.monthlyFee || 0);
  const amount = Math.round(rupees * 100); // paise
  if (amount <= 0) throw new functions.https.HttpsError("failed-precondition", "Fee is not set for your category.");
  const resp = await fetch("https://api.razorpay.com/v1/orders", {
    method: "POST",
    headers: { "Authorization": "Basic " + Buffer.from(keyId + ":" + keySecret).toString("base64"), "Content-Type": "application/json" },
    body: JSON.stringify({ amount, currency: "INR", receipt: `${kind}_${uid}_${Date.now()}`, notes: { uid, kind } })
  });
  const order = await resp.json();
  if (!resp.ok) throw new functions.https.HttpsError("internal", (order.error && order.error.description) || "Could not create order.");
  return { orderId: order.id, amount, currency: "INR", keyId, kind, rupees };
});

/** 2) Verify the payment signature client-side handler sends back, then credit the seller's billing. */
exports.verifyRazorpayPayment = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Please sign in.");
  const uid = context.auth.uid;
  const { razorpay_order_id, razorpay_payment_id, razorpay_signature, kind } = data || {};
  if (!razorpay_order_id || !razorpay_payment_id || !razorpay_signature || !["activation", "monthly"].includes(kind))
    throw new functions.https.HttpsError("invalid-argument", "Missing payment details.");
  const { keySecret } = rzpCfg();
  const expected = crypto.createHmac("sha256", keySecret).update(razorpay_order_id + "|" + razorpay_payment_id).digest("hex");
  if (expected !== razorpay_signature) throw new functions.https.HttpsError("permission-denied", "Payment signature mismatch.");
  const plan = await planFor(uid);
  const rupees = kind === "activation" ? ((plan && plan.activationFee) || 0) : ((plan && plan.monthlyFee) || 0);
  await applyPayment(uid, kind, rupees);
  return { ok: true };
});

/** 3) Webhook backup — Razorpay calls this on payment.captured (reliable even if the browser closes). */
exports.razorpayWebhook = functions.https.onRequest(async (req, res) => {
  const { webhookSecret } = rzpCfg();
  try {
    const sig = req.headers["x-razorpay-signature"];
    const expected = crypto.createHmac("sha256", webhookSecret).update(req.rawBody).digest("hex");
    if (!webhookSecret || sig !== expected) return res.status(400).send("bad signature");
    const event = req.body;
    if (event.event === "payment.captured" || event.event === "order.paid") {
      const ent = (event.payload.payment && event.payload.payment.entity) || {};
      const notes = ent.notes || {};
      if (notes.uid && ["activation", "monthly"].includes(notes.kind)) {
        await applyPayment(notes.uid, notes.kind, (ent.amount || 0) / 100);
      }
    }
    res.json({ ok: true });
  } catch (e) {
    console.error("webhook error", e.message);
    res.status(500).send("error");
  }
});

/* =================== Admin custom claims =================== */
/**
 * Keep an "admin" custom claim in sync with the admins/{uid} collection.
 * Creating an admins doc grants the claim; deleting it revokes the claim.
 * Firestore rules check request.auth.token.admin == true (with the admins-doc
 * lookup kept as a fallback so existing admins keep working immediately).
 * NOTE: the admin must refresh their ID token (re-login or getIdToken(true))
 * for a newly granted claim to take effect.
 */
exports.onAdminDocWrite = functions.firestore
  .document("admins/{uid}")
  .onWrite(async (change, context) => {
    const uid = context.params.uid;
    const isAdmin = change.after.exists;
    try {
      await admin.auth().setCustomUserClaims(uid, { admin: isAdmin });
      await db.collection("users").doc(uid).set(
        { adminClaim: isAdmin, adminClaimAt: admin.firestore.FieldValue.serverTimestamp() },
        { merge: true }
      );
      console.log(`Admin claim ${isAdmin ? "granted" : "revoked"} for ${uid}`);
    } catch (e) {
      console.error("setCustomUserClaims failed for", uid, e.message);
    }
  });
