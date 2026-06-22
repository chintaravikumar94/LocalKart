/* LocalKart Admin Console — Firebase wiring + demo fallback.
 * 1. Paste your Firebase web config below (Firebase Console > Project settings > Web app).
 * 2. Lock access with an `admins` collection or a custom claim (see checkAdmin()).
 * 3. Serve locally:  npx serve .   (or any static host / Firebase Hosting).
 */
const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "YOUR_PROJECT.firebaseapp.com",
  projectId: "YOUR_PROJECT_ID",
  storageBucket: "YOUR_PROJECT.appspot.com",
  messagingSenderId: "YOUR_SENDER_ID",
  appId: "YOUR_APP_ID"
};

let db = null, auth = null, LIVE = false;
try {
  if (firebaseConfig.apiKey !== "YOUR_API_KEY") {
    firebase.initializeApp(firebaseConfig);
    auth = firebase.auth();
    db = firebase.firestore();
    LIVE = true;
  }
} catch (e) { console.warn("Firebase init skipped:", e); }

/* ---------------- AUTH ---------------- */
function showApp(email) {
  document.getElementById("login").style.display = "none";
  document.getElementById("app").style.display = "grid";
  document.getElementById("who").textContent = email || "admin@localkart.app";
  loadAll();
}
function err(msg){ document.getElementById("loginErr").textContent = msg || ""; }

async function checkAdmin(uid) {
  // Gate: only users present in the `admins/{uid}` doc may enter.
  if (!LIVE) return true;
  const doc = await db.collection("admins").doc(uid).get();
  return doc.exists;
}

async function loginEmail() {
  err("");
  const email = document.getElementById("email").value;
  const pass = document.getElementById("password").value;
  if (!LIVE) return showApp(email || "demo-admin@localkart.app"); // demo mode
  try {
    const cred = await auth.signInWithEmailAndPassword(email, pass);
    if (!(await checkAdmin(cred.user.uid))) { err("Not an admin account."); return auth.signOut(); }
    showApp(cred.user.email);
  } catch (e) { err(e.message); }
}

async function loginGoogle() {
  err("");
  if (!LIVE) return showApp("demo-admin@localkart.app");
  try {
    const provider = new firebase.auth.GoogleAuthProvider();
    const cred = await auth.signInWithPopup(provider);
    if (!(await checkAdmin(cred.user.uid))) { err("Not an admin account."); return auth.signOut(); }
    showApp(cred.user.email);
  } catch (e) { err(e.message); }
}

function logout() {
  if (LIVE) auth.signOut();
  document.getElementById("app").style.display = "none";
  document.getElementById("login").style.display = "flex";
}

/* ---------------- NAVIGATION ---------------- */
document.getElementById("nav").addEventListener("click", e => {
  const a = e.target.closest("a"); if (!a) return;
  document.querySelectorAll("#nav a").forEach(x => x.classList.remove("active"));
  document.querySelectorAll(".view").forEach(v => v.classList.remove("active"));
  a.classList.add("active");
  const view = a.dataset.view;
  document.getElementById("view-" + view).classList.add("active");
  document.getElementById("title").textContent = a.textContent.trim().replace(/^[^\w]+/, "");
});

/* ---------------- DATA ---------------- */
const DEMO = {
  stores: [
    {name:"Ravikumar Stores", category:"groceries", rating:4.6, approved:true},
    {name:"City Mobile Care", category:"mobile_repairing", rating:4.3, approved:true},
    {name:"Sri Fancy World", category:"fancy", rating:4.1, approved:false}
  ],
  services: [
    {name:"Ravikumar (Electrician)", category:"electrician", rating:4.8, approved:true},
    {name:"Anil Plumbing", category:"plumber", rating:4.5, approved:false},
    {name:"GreenThumb Garden", category:"gardener", rating:4.2, approved:true}
  ],
  customers: [
    {name:"Suresh K", email:"suresh@example.com", address:"Hyderabad"},
    {name:"Priya R", email:"priya@example.com", address:"Vijayawada"}
  ],
  growItems: [
    {title:"Featured listing", targetRole:"all", ctaText:"Boost"},
    {title:"Pro seller badge", targetRole:"service_provider", ctaText:"Upgrade"}
  ],
  banners: [{title:"Diwali Sale", order:1, active:true},{title:"New in your area", order:2, active:true}]
};

async function fetchCol(name, fallback) {
  if (!LIVE) return fallback.map((d, i) => ({ id: `demo-${name}-${i}`, ...d }));
  try {
    const s = await db.collection(name).limit(200).get();
    return s.docs.map(d => ({ id: d.id, ...d.data() }));
  } catch (e) { console.warn(name, e); return fallback.map((d, i) => ({ id: `demo-${name}-${i}`, ...d })); }
}
const tag = ok => ok ? '<span class="tag ok">Approved</span>' : '<span class="tag wait">Pending</span>';

// Holds the current pending list so approve/reject know the collection + id.
let PENDING = [];

async function loadAll() {
  const stores = await fetchCol("stores", DEMO.stores);
  const services = await fetchCol("services", DEMO.services);
  const customers = await fetchCol("users", DEMO.customers);
  const grow = await fetchCol("growItems", DEMO.growItems);
  const banners = await fetchCol("banners", DEMO.banners);

  PENDING = [
    ...stores.filter(x => !x.approved).map(x => ({ ...x, _col: "stores" })),
    ...services.filter(x => !x.approved).map(x => ({ ...x, _col: "services" })),
  ];

  document.getElementById("c-customers").textContent = LIVE ? customers.length : "50,00,000*";
  document.getElementById("c-stores").textContent = stores.length;
  document.getElementById("c-services").textContent = services.length;
  document.getElementById("c-pending").textContent = PENDING.length;

  const svcNames = new Set(services.map(s => s.name));
  document.getElementById("recent").innerHTML =
    [...stores, ...services].slice(0, 6).map(s =>
      `<tr><td>${s.name}</td><td>${svcNames.has(s.name) ? "Service" : "Store"}</td><td>${s.category || "-"}</td><td>${tag(s.approved)}</td></tr>`).join("");

  document.getElementById("storeRows").innerHTML = stores.map(s =>
    `<tr><td>${s.name}</td><td>${s.category}</td><td>⭐ ${(s.rating || 0).toFixed ? s.rating.toFixed(1) : s.rating}</td><td>${tag(s.approved)}</td></tr>`).join("");
  document.getElementById("svcRows").innerHTML = services.map(s =>
    `<tr><td>${s.name}</td><td>${s.category}</td><td>⭐ ${(s.rating || 0).toFixed ? s.rating.toFixed(1) : s.rating}</td><td>${tag(s.approved)}</td></tr>`).join("");
  document.getElementById("custRows").innerHTML = customers.map(c =>
    `<tr><td>${c.name || "-"}</td><td>${c.email || "-"}</td><td>${c.address || "-"}</td></tr>`).join("");
  document.getElementById("growRows").innerHTML = grow.map(g =>
    `<tr><td>${g.title}</td><td>${g.targetRole}</td><td>${g.ctaText}</td></tr>`).join("");
  document.getElementById("bannerRows").innerHTML = banners.map(b =>
    `<tr><td>${b.title}</td><td>${b.order}</td><td>${b.active ? "Yes" : "No"}</td></tr>`).join("");

  document.getElementById("approvalRows").innerHTML = PENDING.length ? PENDING.map((p, i) =>
    `<tr><td>${p.name}</td><td>${p._col === "services" ? "Service" : "Store"}</td><td>${p.category}</td>
      <td><button class="approve" onclick="approve(${i})">Approve</button>
          <button class="reject" onclick="reject(${i})">Reject</button></td></tr>`).join("")
    : `<tr><td colspan="4" style="color:var(--muted)">No pending approvals 🎉</td></tr>`;
}

async function setApproval(i, approved) {
  const p = PENDING[i];
  if (!p) return;
  if (!LIVE) { alert(`(demo) ${approved ? "Approved" : "Rejected"} ${p.name}. Paste your Firebase config in app.js to make this live.`); return; }
  try {
    await db.collection(p._col).doc(p.id).update({ approved });
    await loadAll();
  } catch (e) { alert("Update failed: " + e.message); }
}
function approve(i) { setApproval(i, true); }
function reject(i) { setApproval(i, false); }

// Keep session on reload
if (LIVE) auth.onAuthStateChanged(async u => {
  if (u && await checkAdmin(u.uid)) showApp(u.email);
});
