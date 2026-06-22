/* LocalKart Admin Console — live Firebase dashboard. */
const firebaseConfig = {
  apiKey: "AIzaSyC4h_btIhf5cGkMFm2N92j8RsdBfNt7DZY",
  authDomain: "localkart-7dfb4.firebaseapp.com",
  projectId: "localkart-7dfb4",
  storageBucket: "localkart-7dfb4.firebasestorage.app",
  messagingSenderId: "537191050226",
  appId: "1:537191050226:web:f4215514741ee93cbb196b"
};

let db = null, auth = null, storage = null, LIVE = false;
try {
  if (firebaseConfig.apiKey !== "YOUR_API_KEY") {
    firebase.initializeApp(firebaseConfig);
    auth = firebase.auth();
    db = firebase.firestore();
    storage = firebase.storage();
    LIVE = true;
  }
} catch (e) { console.warn("Firebase init skipped:", e); }

/** Uploads the chosen file to Storage and returns its download URL ("" if none). */
async function uploadImage(fileInputId, folder) {
  const f = document.getElementById(fileInputId)?.files?.[0];
  if (!f || !LIVE) return "";
  const ref = storage.ref().child(`${folder}/${Date.now()}_${f.name.replace(/\s+/g, "_")}`);
  await ref.put(f);
  return await ref.getDownloadURL();
}

/** Live preview for a file input -> <img>. */
function previewFile(inputId, imgId) {
  const f = document.getElementById(inputId)?.files?.[0];
  const img = document.getElementById(imgId);
  if (f && img) { img.src = URL.createObjectURL(f); img.style.display = "block"; }
}

/* ---------------- TOAST ---------------- */
function toast(msg, kind) {
  const t = document.createElement("div");
  t.className = "toast " + (kind || "");
  t.textContent = msg;
  document.getElementById("toast").appendChild(t);
  setTimeout(() => t.remove(), 3200);
}

/* ---------------- AUTH ---------------- */
function err(m) { document.getElementById("loginErr").textContent = m || ""; }

function showApp(user) {
  document.getElementById("login").style.display = "none";
  document.getElementById("app").style.display = "grid";
  const email = user?.email || "admin@localkart.app";
  document.getElementById("me-email").textContent = email;
  document.getElementById("me-name").textContent = user?.displayName || "Admin";
  document.getElementById("me-av").textContent = (email[0] || "A").toUpperCase();
  loadAll();
}

async function checkAdmin(uid) {
  if (!LIVE) return true;
  const doc = await db.collection("admins").doc(uid).get();
  return doc.exists;
}

async function loginGoogle() {
  err("");
  if (!LIVE) return showApp({ email: "demo-admin@localkart.app" });
  try {
    const cred = await auth.signInWithPopup(new firebase.auth.GoogleAuthProvider());
    if (!(await checkAdmin(cred.user.uid))) { err("Not an admin account."); return auth.signOut(); }
    showApp(cred.user);
  } catch (e) { err(e.message); }
}

async function loginEmail() {
  err("");
  const email = document.getElementById("email").value;
  const pass = document.getElementById("password").value;
  if (!LIVE) return showApp({ email });
  try {
    const cred = await auth.signInWithEmailAndPassword(email, pass);
    if (!(await checkAdmin(cred.user.uid))) { err("Not an admin account."); return auth.signOut(); }
    showApp(cred.user);
  } catch (e) { err(e.message); }
}

function logout() {
  if (LIVE) auth.signOut();
  document.getElementById("app").style.display = "none";
  document.getElementById("login").style.display = "flex";
}

/* ---------------- NAV ---------------- */
const CRUMBS = {
  dashboard: "Overview of your marketplace", approvals: "Review & publish new sellers",
  stores: "All registered stores", services: "All service providers", products: "Catalog items",
  orders: "Customer orders", bookings: "Service bookings", reviews: "Customer feedback",
  customers: "Registered users", grow: "Promos shown to sellers", banners: "Home banners"
};
document.getElementById("nav").addEventListener("click", e => {
  const a = e.target.closest("a"); if (!a) return;
  document.querySelectorAll("#nav a").forEach(x => x.classList.remove("active"));
  document.querySelectorAll(".view").forEach(v => v.classList.remove("active"));
  a.classList.add("active");
  const v = a.dataset.view;
  document.getElementById("view-" + v).classList.add("active");
  document.getElementById("title").textContent = a.textContent.trim().replace(/\s+\d+$/, "");
  document.getElementById("crumb").textContent = CRUMBS[v] || "";
});

/* ---------------- DATA ---------------- */
const DEMO = {
  stores: [
    { name: "Ravikumar Grocery", category: "groceries", rating: 4.6, approved: true, isOpen: true, address: "Madhapur" },
    { name: "City Mobile Care", category: "mobile_repairing", rating: 4.3, approved: true, isOpen: true, address: "Kondapur" },
    { name: "Sri Fancy World", category: "fancy", rating: 4.1, approved: false, isOpen: true, address: "Gachibowli" }
  ],
  services: [
    { name: "Ravi Electricals", category: "electrician", rating: 4.8, approved: true, available: true, address: "Kondapur" },
    { name: "Anil Plumbing", category: "plumber", rating: 4.5, approved: false, available: true, address: "Madhapur" }
  ],
  products: [{ name: "Rice 5kg", price: 320, mrp: 360, inStock: true }],
  orders: [{ id: "demo1", total: 594, items: [{ name: "Rice", qty: 1 }], status: "PENDING" }],
  bookings: [{ service: "Wiring fix", status: "NEW" }],
  reviews: [{ customerName: "Suresh", rating: 5, comment: "Great!" }],
  users: [{ name: "Suresh K", email: "suresh@example.com", address: "Hyderabad" }],
  growItems: [{ title: "Featured listing", targetRole: "all", ctaText: "Boost" }],
  banners: [{ title: "Diwali Sale", order: 1, active: true }]
};

const DATA = {};
let PENDING = [];

async function fetchCol(name, fb) {
  if (!LIVE) return fb.map((d, i) => ({ id: `demo-${name}-${i}`, ...d }));
  try { const s = await db.collection(name).limit(300).get(); return s.docs.map(d => ({ id: d.id, ...d.data() })); }
  catch (e) { console.warn(name, e); return []; }
}

async function loadAll() {
  const [stores, services, products, orders, bookings, reviews, users, grow, banners] = await Promise.all([
    fetchCol("stores", DEMO.stores), fetchCol("services", DEMO.services), fetchCol("products", DEMO.products),
    fetchCol("orders", DEMO.orders), fetchCol("bookings", DEMO.bookings), fetchCol("reviews", DEMO.reviews),
    fetchCol("users", DEMO.users), fetchCol("growItems", DEMO.growItems), fetchCol("banners", DEMO.banners)
  ]);
  Object.assign(DATA, { stores, services, products, orders, bookings, reviews, users, grow, banners });
  PENDING = [
    ...stores.filter(x => !x.approved).map(x => ({ ...x, _col: "stores" })),
    ...services.filter(x => !x.approved).map(x => ({ ...x, _col: "services" }))
  ];
  render();
}

/* ---------------- RENDER ---------------- */
const tag = (ok) => ok ? '<span class="tag ok">Approved</span>' : '<span class="tag wait">Pending</span>';
const num = (n) => (n || 0).toLocaleString("en-IN");
const money = (n) => "₹" + num(Math.round(n || 0));
const star = (r) => '<span class="stars">' + "★".repeat(Math.round(r || 0)).padEnd(5, "☆") + "</span>";
const img = (url) => url ? `<img class="avatar" src="${url}">` : `<div class="avatar" style="display:inline-grid;place-items:center">·</div>`;
const filt = (id) => (document.getElementById(id)?.value || "").toLowerCase();

let chartCat, chartOrders;

function render() {
  const { stores = [], services = [], products = [], orders = [], bookings = [], reviews = [], users = [], grow = [], banners = [] } = DATA;

  // KPIs
  const revenue = orders.filter(o => o.status === "COMPLETED").reduce((s, o) => s + (o.total || 0), 0);
  document.getElementById("k-customers").textContent = LIVE ? num(users.length) : "50,00,000*";
  document.getElementById("k-stores").textContent = num(stores.length);
  document.getElementById("k-services").textContent = num(services.length);
  document.getElementById("k-pending").textContent = num(PENDING.length);
  document.getElementById("k-orders").textContent = num(orders.length);
  document.getElementById("k-revenue").textContent = money(revenue);
  document.getElementById("badge-pending").textContent = PENDING.length || "";

  // Recent
  const svcNames = new Set(services.map(s => s.name));
  document.getElementById("recent").innerHTML = [...stores, ...services].slice(0, 7).map(s =>
    `<tr><td>${s.name}</td><td>${svcNames.has(s.name) ? "Service" : "Store"}</td><td>${s.category || "-"}</td><td>${star(s.rating)}</td><td>${tag(s.approved)}</td></tr>`).join("") || emptyRow(5);

  // Approvals
  document.getElementById("approvalRows").innerHTML = PENDING.length ? PENDING.map((p, i) =>
    `<tr><td>${p.name}</td><td>${p._col === "services" ? "Service" : "Store"}</td><td>${p.category || "-"}</td><td>${p.address || "-"}</td>
      <td class="row-actions"><button class="approve" onclick="approve(${i})">Approve</button>
      <button class="reject" onclick="reject(${i})">Reject</button></td></tr>`).join("")
    : `<tr><td colspan="5" class="empty">No pending approvals 🎉</td></tr>`;

  // Stores
  const qs = filt("q-stores");
  document.getElementById("storeRows").innerHTML = stores.filter(s => (s.name || "").toLowerCase().includes(qs)).map(s =>
    `<tr><td>${img(s.photoUrl)}</td><td>${s.name}</td><td>${s.category || "-"}</td><td>${star(s.rating)} ${(s.rating || 0).toFixed(1)}</td>
      <td>${s.isOpen ? '<span class="tag ok">Open</span>' : '<span class="tag no">Closed</span>'}</td><td>${tag(s.approved)}</td>
      <td>${s.approved ? `<button class="mini" onclick="setApprovalById('stores','${s.id}',false)">Unpublish</button>` : `<button class="approve" onclick="setApprovalById('stores','${s.id}',true)">Approve</button>`}</td></tr>`).join("") || emptyRow(7);

  // Services
  const qv = filt("q-svc");
  document.getElementById("svcRows").innerHTML = services.filter(s => (s.name || "").toLowerCase().includes(qv)).map(s =>
    `<tr><td>${img(s.photoUrl)}</td><td>${s.name}</td><td>${s.category || "-"}</td><td>${star(s.rating)} ${(s.rating || 0).toFixed(1)}</td>
      <td>${s.available ? '<span class="tag ok">Yes</span>' : '<span class="tag no">No</span>'}</td><td>${tag(s.approved)}</td>
      <td>${s.approved ? `<button class="mini" onclick="setApprovalById('services','${s.id}',false)">Unpublish</button>` : `<button class="approve" onclick="setApprovalById('services','${s.id}',true)">Approve</button>`}</td></tr>`).join("") || emptyRow(7);

  // Products
  const qp = filt("q-prod");
  document.getElementById("prodRows").innerHTML = products.filter(p => (p.name || "").toLowerCase().includes(qp)).map(p =>
    `<tr><td>${img(p.imageUrl)}</td><td>${p.name}</td><td>${money(p.price)}</td><td>${p.mrp ? money(p.mrp) : "-"}</td>
      <td>${p.inStock ? '<span class="tag ok">Yes</span>' : '<span class="tag no">No</span>'}</td></tr>`).join("") || emptyRow(5);

  // Orders
  document.getElementById("orderRows").innerHTML = orders.map(o =>
    `<tr><td>${(o.id || "").slice(0, 6).toUpperCase()}</td><td>${(o.items || []).map(i => `${i.name}×${i.qty}`).join(", ") || "-"}</td>
      <td>${money(o.total)}</td><td>${statusTag(o.status)}</td></tr>`).join("") || emptyRow(4);

  // Bookings
  document.getElementById("bookingRows").innerHTML = bookings.map(b =>
    `<tr><td>${b.service || "Service"}</td><td>${fmtTs(b.scheduledAt)}</td><td>${statusTag(b.status)}</td></tr>`).join("") || emptyRow(3);

  // Reviews
  document.getElementById("reviewRows").innerHTML = reviews.map(r =>
    `<tr><td>${r.customerName || "Customer"}</td><td>${star(r.rating)}</td><td>${r.comment || "-"}</td></tr>`).join("") || emptyRow(3);

  // Customers
  const qc = filt("q-cust");
  document.getElementById("custRows").innerHTML = users.filter(u => ((u.name || "") + (u.email || "")).toLowerCase().includes(qc)).map(u =>
    `<tr><td>${img(u.photoUrl)}</td><td>${u.name || "-"}</td><td>${u.email || "-"}</td><td>${u.address || "-"}</td></tr>`).join("") || emptyRow(4);

  // Grow
  document.getElementById("growRows").innerHTML = grow.map(g =>
    `<tr><td>${g.title}</td><td>${g.targetRole}</td><td>${g.ctaText || "-"}</td>
      <td class="row-actions"><button class="mini" onclick="editGrow('${g.id}')">Edit</button>
      <button class="reject" onclick="del('growItems','${g.id}')">Delete</button></td></tr>`).join("") || emptyRow(4);

  // Banners
  document.getElementById("bannerRows").innerHTML = banners.map(b =>
    `<tr><td>${img(b.imageUrl)} ${b.title}</td><td><span class="tag info">${b.audience || "customer"}</span></td><td>${b.order ?? "-"}</td><td>${b.active ? '<span class="tag ok">Yes</span>' : '<span class="tag no">No</span>'}</td>
      <td class="row-actions"><button class="mini" onclick="editBanner('${b.id}')">Edit</button>
      <button class="mini" onclick="toggleBanner('${b.id}',${!b.active})">${b.active ? "Disable" : "Enable"}</button>
      <button class="reject" onclick="del('banners','${b.id}')">Delete</button></td></tr>`).join("") || emptyRow(5);

  drawCharts();
}

function emptyRow(cols) { return `<tr><td colspan="${cols}" class="empty">No data yet</td></tr>`; }
function statusTag(s) {
  s = (s || "").toUpperCase();
  if (["COMPLETED", "DONE", "CONFIRMED"].includes(s)) return `<span class="tag ok">${s}</span>`;
  if (["CANCELLED", "REJECTED"].includes(s)) return `<span class="tag no">${s}</span>`;
  if (["ACTIVE", "IN_PROGRESS"].includes(s)) return `<span class="tag info">${s}</span>`;
  return `<span class="tag wait">${s || "—"}</span>`;
}
function fmtTs(ts) {
  if (!ts) return "-";
  const ms = ts.seconds ? ts.seconds * 1000 : (ts._seconds ? ts._seconds * 1000 : null);
  return ms ? new Date(ms).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "-";
}

function drawCharts() {
  const { stores = [], services = [], orders = [] } = DATA;
  const catCounts = {};
  [...stores, ...services].forEach(s => { catCounts[s.category || "other"] = (catCounts[s.category || "other"] || 0) + 1; });
  const css = getComputedStyle(document.body);
  const palette = ["#2563EB", "#7C3AED", "#F59E0B", "#22c55e", "#ef4444", "#06b6d4", "#ec4899", "#84cc16"];

  if (chartCat) chartCat.destroy();
  chartCat = new Chart(document.getElementById("chartCat"), {
    type: "doughnut",
    data: { labels: Object.keys(catCounts), datasets: [{ data: Object.values(catCounts), backgroundColor: palette }] },
    options: { plugins: { legend: { labels: { color: "#93a4c0" }, position: "right" } } }
  });

  const statuses = ["PENDING", "ACTIVE", "COMPLETED", "CANCELLED"];
  const counts = statuses.map(st => orders.filter(o => (o.status || "").toUpperCase() === st).length);
  if (chartOrders) chartOrders.destroy();
  chartOrders = new Chart(document.getElementById("chartOrders"), {
    type: "bar",
    data: { labels: statuses, datasets: [{ data: counts, backgroundColor: palette, borderRadius: 6 }] },
    options: { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: "#93a4c0" }, grid: { display: false } }, y: { ticks: { color: "#93a4c0" }, grid: { color: "#22324f" } } } }
  });
}

/* ---------------- ACTIONS ---------------- */
async function setApprovalById(col, id, approved) {
  if (!LIVE) { toast(`(demo) ${approved ? "Approved" : "Unpublished"}`, "ok"); return; }
  try { await db.collection(col).doc(id).update({ approved }); toast(approved ? "Seller approved" : "Seller unpublished", "ok"); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
function approve(i) { const p = PENDING[i]; if (p) setApprovalById(p._col, p.id, true); }
function reject(i) { const p = PENDING[i]; if (p) setApprovalById(p._col, p.id, false); }

async function del(col, id) {
  if (!confirm("Delete this item?")) return;
  if (!LIVE) { toast("(demo) deleted", "ok"); return; }
  try { await db.collection(col).doc(id).delete(); toast("Deleted", "ok"); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
async function toggleBanner(id, active) {
  if (!LIVE) { toast("(demo) toggled", "ok"); return; }
  try { await db.collection("banners").doc(id).update({ active }); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}

/* ---------------- MODALS (add grow / banner) ---------------- */
function modal(html) { document.getElementById("modalBox").innerHTML = html; document.getElementById("modalBg").classList.add("open"); }
function closeModal() { document.getElementById("modalBg").classList.remove("open"); }
document.getElementById("modalBg").addEventListener("click", e => { if (e.target.id === "modalBg") closeModal(); });

let editGrowId = null;
function editGrow(id) { openGrow(DATA.grow.find(x => x.id === id)); }
function openGrow(g) {
  editGrowId = g?.id || null;
  const role = g?.targetRole || "all";
  modal(`<h3>${g ? "Edit" : "Add"} grow item</h3>
    <label>Title</label><input id="g-title" value="${g?.title || ""}" placeholder="e.g. Featured listing">
    <label>Description</label><input id="g-desc" value="${g?.description || ""}" placeholder="Short description">
    <label>CTA text</label><input id="g-cta" value="${g?.ctaText || ""}" placeholder="e.g. Boost">
    <label>Target audience</label>
    <select id="g-role">${opt("all", "All sellers", role)}${opt("store_owner", "Store owners", role)}${opt("service_provider", "Service providers", role)}</select>
    <label>Image (optional)${g ? " — leave empty to keep current" : ""}</label>
    <input id="g-file" type="file" accept="image/*" onchange="previewFile('g-file','g-prev')">
    <img id="g-prev" src="${g?.imageUrl || ""}" style="display:${g?.imageUrl ? "block" : "none"};width:100%;height:120px;object-fit:cover;border-radius:10px;margin-top:8px">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" id="g-save" onclick="saveGrow()">Save</button></div>`);
}
async function saveGrow() {
  const title = val("g-title");
  if (!title) return toast("Title required", "bad");
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  const btn = document.getElementById("g-save"); if (btn) { btn.disabled = true; btn.textContent = "Saving…"; }
  try {
    const imageUrl = await uploadImage("g-file", "growItems");
    const data = {
      title, description: val("g-desc"), ctaText: val("g-cta"),
      targetRole: document.getElementById("g-role")?.value || "all"
    };
    if (imageUrl) data.imageUrl = imageUrl;
    if (editGrowId) {
      await db.collection("growItems").doc(editGrowId).update(data);
      toast("Grow item updated", "ok");
    } else {
      await db.collection("growItems").add({ ...data, imageUrl: imageUrl || "" });
      toast("Grow item added", "ok");
    }
    closeModal(); await loadAll();
  } catch (e) { toast("Failed: " + e.message, "bad"); if (btn) { btn.disabled = false; btn.textContent = "Save"; } }
}

function opt(v, label, cur) { return `<option value="${v}" ${v === cur ? "selected" : ""}>${label}</option>`; }

let editBannerId = null;
function editBanner(id) { openBanner(DATA.banners.find(x => x.id === id)); }
function openBanner(b) {
  editBannerId = b?.id || null;
  const aud = b?.audience || "customer";
  modal(`<h3>${b ? "Edit" : "Add"} banner</h3>
    <label>Title</label><input id="b-title" value="${b?.title || ""}" placeholder="e.g. Diwali Sale">
    <label>Show in app</label>
    <select id="b-aud">${opt("customer", "Customer app", aud)}${opt("seller", "Seller app", aud)}${opt("both", "Both apps", aud)}</select>
    <label>Banner image ${b ? "(leave empty to keep current)" : ""}</label>
    <input id="b-file" type="file" accept="image/*" onchange="previewFile('b-file','b-prev')">
    <img id="b-prev" src="${b?.imageUrl || ""}" style="display:${b?.imageUrl ? "block" : "none"};width:100%;height:120px;object-fit:cover;border-radius:10px;margin-top:8px">
    <label>Order</label><input id="b-order" type="number" value="${b?.order ?? 1}">
    <label>Target (optional)</label><input id="b-target" value="${b?.target || ""}" placeholder="category or deep link">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" id="b-save" onclick="saveBanner()">Save</button></div>`);
}
async function saveBanner() {
  const title = val("b-title");
  if (!title) return toast("Title required", "bad");
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  const btn = document.getElementById("b-save"); if (btn) { btn.disabled = true; btn.textContent = "Saving…"; }
  try {
    const imageUrl = await uploadImage("b-file", "banners");
    const data = {
      title, target: val("b-target"),
      audience: document.getElementById("b-aud")?.value || "customer",
      order: parseInt(val("b-order") || "1", 10)
    };
    if (imageUrl) data.imageUrl = imageUrl;
    if (editBannerId) {
      await db.collection("banners").doc(editBannerId).update(data);
      toast("Banner updated", "ok");
    } else {
      await db.collection("banners").add({ ...data, imageUrl: imageUrl || "", active: true });
      toast("Banner added", "ok");
    }
    closeModal(); await loadAll();
  } catch (e) { toast("Failed: " + e.message, "bad"); if (btn) { btn.disabled = false; btn.textContent = "Save"; } }
}
function val(id) { return (document.getElementById(id)?.value || "").trim(); }

/* ---------------- SESSION ---------------- */
if (LIVE) auth.onAuthStateChanged(async u => { if (u && await checkAdmin(u.uid)) showApp(u); });
