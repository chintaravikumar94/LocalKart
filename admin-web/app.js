/* LocalKart Admin Console — full control center for both apps. */
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
    auth = firebase.auth(); db = firebase.firestore(); storage = firebase.storage(); LIVE = true;
  }
} catch (e) { console.warn("init skipped", e); }

/* ---------- theme ---------- */
function applyTheme(t) {
  document.body.classList.toggle("dark", t === "dark");
  const b = document.getElementById("themeBtn");
  if (b) b.textContent = t === "dark" ? "☀️ Light" : "🌙 Dark";
}
function toggleTheme() {
  const next = document.body.classList.contains("dark") ? "light" : "dark";
  try { localStorage.setItem("lk-theme", next); } catch (e) {}
  applyTheme(next);
}
applyTheme((() => { try { return localStorage.getItem("lk-theme"); } catch (e) { return null; } })() || "light");

/* ---------- helpers ---------- */
function toast(msg, kind) { const t = document.createElement("div"); t.className = "toast " + (kind || ""); t.textContent = msg; document.getElementById("toast").appendChild(t); setTimeout(() => t.remove(), 3200); }
function modal(html) { document.getElementById("modalBox").innerHTML = html; document.getElementById("modalBg").classList.add("open"); }
function closeModal() { document.getElementById("modalBg").classList.remove("open"); }
document.getElementById("modalBg").addEventListener("click", e => { if (e.target.id === "modalBg") closeModal(); });
function opt(v, label, cur) { return `<option value="${v}" ${v === cur ? "selected" : ""}>${label}</option>`; }
function val(id) { return (document.getElementById(id)?.value || "").trim(); }
const tag = ok => ok ? '<span class="tag ok">Approved</span>' : '<span class="tag wait">Pending</span>';
const num = n => (n || 0).toLocaleString("en-IN");
const money = n => "₹" + num(Math.round(n || 0));
const esc = s => (s == null ? "" : String(s).replace(/"/g, "&quot;"));
const star = r => '<span class="stars">' + "★".repeat(Math.round(r || 0)).padEnd(5, "☆") + "</span>";
const img = u => u ? `<img class="avatar" src="${u}">` : `<span class="avatar" style="display:inline-grid;place-items:center">·</span>`;
const filt = id => (document.getElementById(id)?.value || "").toLowerCase();

/* ---------- auth ---------- */
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
async function checkAdmin(uid) { if (!LIVE) return true; const d = await db.collection("admins").doc(uid).get(); return d.exists; }
async function loginGoogle() {
  err(""); if (!LIVE) return showApp({ email: "demo-admin@localkart.app" });
  try { const c = await auth.signInWithPopup(new firebase.auth.GoogleAuthProvider()); if (!(await checkAdmin(c.user.uid))) { err("Not an admin account."); return auth.signOut(); } showApp(c.user); }
  catch (e) { err(e.message); }
}
async function loginEmail() {
  err(""); const email = val("email"), pass = document.getElementById("password").value;
  if (!LIVE) return showApp({ email });
  try { const c = await auth.signInWithEmailAndPassword(email, pass); if (!(await checkAdmin(c.user.uid))) { err("Not an admin account."); return auth.signOut(); } showApp(c.user); }
  catch (e) { err(e.message); }
}
function logout() { if (LIVE) auth.signOut(); document.getElementById("app").style.display = "none"; document.getElementById("login").style.display = "flex"; }

/* ---------- nav ---------- */
const CRUMBS = {
  dashboard: "Overview of your marketplace", approvals: "Review & publish new sellers", stores: "Manage stores",
  services: "Manage service providers", products: "Catalog items", categories: "Store & service categories",
  orders: "Customer orders", bookings: "Service bookings", appointments: "Store appointments",
  requests: "Service / job requests", reviews: "Customer feedback", customers: "Registered users",
  admins: "Console administrators", notifications: "Broadcast to apps", grow: "Seller promos", banners: "Home banners",
  catalog: "Company master catalog"
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

/* ---------- data ---------- */
const DEMO = {
  stores: [{ name: "Ravikumar Grocery", category: "groceries", rating: 4.6, approved: true, isOpen: true, address: "Madhapur" }],
  services: [{ name: "Ravi Electricals", category: "electrician", rating: 4.8, approved: true, available: true, address: "Kondapur" }],
  products: [{ name: "Rice 5kg", price: 320, mrp: 360, inStock: true }],
  orders: [{ id: "demo1", total: 594, items: [{ name: "Rice", qty: 1 }], status: "PENDING" }],
  bookings: [{ service: "Wiring", status: "NEW" }], appointments: [{ purpose: "Meeseva", status: "NEW" }],
  serviceRequests: [{ title: "Fix tap", details: "Leaking", status: "NEW" }],
  reviews: [{ customerName: "Suresh", rating: 5, comment: "Great!" }],
  users: [{ name: "Suresh K", email: "suresh@example.com", role: "CUSTOMER" }],
  admins: [], categories: [{ name: "groceries", type: "store" }],
  growItems: [{ title: "Featured listing", targetRole: "all", ctaText: "Boost" }],
  banners: [{ title: "Diwali Sale", order: 1, active: true, audience: "customer" }]
};
const DATA = {}; let PENDING = []; let BSETTINGS = { cornerStyle: "rounded", border: false, heightStyle: "medium", template: "full" };

async function fetchCol(name, fb) {
  if (!LIVE) return (fb || []).map((d, i) => ({ id: `demo-${name}-${i}`, ...d }));
  try { const s = await db.collection(name).limit(500).get(); return s.docs.map(d => ({ id: d.id, ...d.data() })); }
  catch (e) { console.warn(name, e); return []; }
}
async function loadBannerSettings() { if (!LIVE) return; try { const d = await db.collection("settings").doc("banners").get(); if (d.exists) BSETTINGS = { ...BSETTINGS, ...d.data() }; } catch (e) {} }

async function loadAll() {
  const [stores, services, products, orders, bookings, appointments, serviceRequests, reviews, users, admins, categories, grow, banners, catalog, offerings] = await Promise.all([
    fetchCol("stores", DEMO.stores), fetchCol("services", DEMO.services), fetchCol("products", DEMO.products),
    fetchCol("orders", DEMO.orders), fetchCol("bookings", DEMO.bookings), fetchCol("appointments", DEMO.appointments),
    fetchCol("serviceRequests", DEMO.serviceRequests), fetchCol("reviews", DEMO.reviews), fetchCol("users", DEMO.users),
    fetchCol("admins", DEMO.admins), fetchCol("categories", DEMO.categories), fetchCol("growItems", DEMO.growItems), fetchCol("banners", DEMO.banners),
    fetchCol("catalog", []), fetchCol("serviceOfferings", [])
  ]);
  await loadBannerSettings();
  Object.assign(DATA, { stores, services, products, orders, bookings, appointments, serviceRequests, reviews, users, admins, categories, grow, banners, catalog, offerings });
  PENDING = [...stores.filter(x => !x.approved).map(x => ({ ...x, _col: "stores" })), ...services.filter(x => !x.approved).map(x => ({ ...x, _col: "services" }))];
  render();
}

/* ---------- render ---------- */
function statusSelect(col, id, cur, opts) {
  return `<select onchange="changeStatus('${col}','${id}',this.value)">${opts.map(o => opt(o, o, cur)).join("")}</select>`;
}
const ORDER_ST = ["PENDING", "ACTIVE", "COMPLETED", "CANCELLED"];
const BOOK_ST = ["NEW", "CONFIRMED", "DONE", "CANCELLED"];
const REQ_ST = ["NEW", "IN_PROGRESS", "DONE", "REJECTED"];
function fmtTs(ts) { if (!ts) return "-"; const ms = ts.seconds ? ts.seconds * 1000 : (ts._seconds ? ts._seconds * 1000 : null); return ms ? new Date(ms).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" }) : "-"; }
const ROLES = ["CUSTOMER", "STORE_OWNER", "SERVICE_PROVIDER", "STORE_AND_PROVIDER", "ADMIN"];
let chartCat, chartOrders;

function render() {
  const D = DATA;
  const revenue = (D.orders || []).filter(o => o.status === "COMPLETED").reduce((s, o) => s + (o.total || 0), 0);
  document.getElementById("k-customers").textContent = LIVE ? num((D.users || []).length) : "50,00,000*";
  document.getElementById("k-stores").textContent = num((D.stores || []).length);
  document.getElementById("k-services").textContent = num((D.services || []).length);
  document.getElementById("k-pending").textContent = num(PENDING.length);
  document.getElementById("k-orders").textContent = num((D.orders || []).length);
  document.getElementById("k-revenue").textContent = money(revenue);
  document.getElementById("badge-pending").textContent = PENDING.length || "";

  const svcNames = new Set((D.services || []).map(s => s.name));
  document.getElementById("recent").innerHTML = [...(D.stores || []), ...(D.services || [])].slice(0, 7).map(s =>
    `<tr><td>${s.name}</td><td>${svcNames.has(s.name) ? "Service" : "Store"}</td><td>${s.category || "-"}</td><td>${star(s.rating)}</td><td>${tag(s.approved)}</td></tr>`).join("") || empty(5);

  document.getElementById("approvalRows").innerHTML = PENDING.length ? PENDING.map((p, i) =>
    `<tr><td>${p.name}</td><td>${p._col === "services" ? "Service" : "Store"}</td><td>${p.category || "-"}</td><td>${p.address || "-"}</td>
     <td class="row-actions"><button class="approve" onclick="setApprovalById('${p._col}','${p.id}',true)">Approve</button>
     <button class="reject" onclick="setApprovalById('${p._col}','${p.id}',false)">Reject</button></td></tr>`).join("")
    : `<tr><td colspan="5" class="empty">No pending approvals 🎉</td></tr>`;

  renderListing("stores", "storeRows", "q-stores", "isOpen", "Open", "Closed");
  renderListing("services", "svcRows", "q-svc", "available", "Yes", "No");

  const qp = filt("q-prod");
  document.getElementById("prodRows").innerHTML = (D.products || []).filter(p => (p.name || "").toLowerCase().includes(qp)).map(p =>
    `<tr><td>${img(p.imageUrl)}</td><td>${p.name}</td><td>${money(p.price)}</td><td>${p.mrp ? money(p.mrp) : "-"}</td>
     <td>${p.inStock ? '<span class="tag ok">Yes</span>' : '<span class="tag no">No</span>'}</td>
     <td>${tag(p.approved)}</td>
     <td class="row-actions">
       ${p.approved ? `<button class="mini" onclick="toggleField('products','${p.id}','approved',false)">Unpublish</button>` : `<button class="approve" onclick="toggleField('products','${p.id}','approved',true)">Approve</button>`}
       <button class="mini" onclick="toggleField('products','${p.id}','inStock',${!p.inStock})">${p.inStock ? "Mark out" : "In stock"}</button>
       <button class="reject" onclick="del('products','${p.id}')">Delete</button></td></tr>`).join("") || empty(7);

  document.getElementById("catalogRows").innerHTML = (D.catalog || []).map(c =>
    `<tr><td>${img(c.imageUrl)}</td><td>${c.name}</td><td><span class="tag info">${c.type}</span></td><td>${c.category || "-"}</td>
     <td>${c.suggestedMrp ? money(c.suggestedMrp) : "-"}</td>
     <td class="row-actions"><button class="mini" onclick="editCatalogItem('${c.id}')">Edit</button>
     <button class="reject" onclick="del('catalog','${c.id}')">Delete</button></td></tr>`).join("") || empty(6);

  document.getElementById("catRows").innerHTML = (D.categories || []).map(c =>
    `<tr><td>${c.name}</td><td><span class="tag info">${c.type || "store"}</span></td>
     <td><button class="reject" onclick="del('categories','${c.id}')">Delete</button></td></tr>`).join("") || empty(3);

  const qo = filt("q-ord");
  document.getElementById("orderRows").innerHTML = (D.orders || []).filter(o => (o.id || "").toLowerCase().includes(qo) || (o.items || []).some(i => (i.name || "").toLowerCase().includes(qo))).map(o =>
    `<tr><td>${(o.id || "").slice(0, 6).toUpperCase()}</td><td>${(o.items || []).map(i => `${i.name}×${i.qty}`).join(", ") || "-"}</td>
     <td>${money(o.total)}</td><td>${statusSelect("orders", o.id, o.status, ORDER_ST)}</td></tr>`).join("") || empty(4);

  document.getElementById("bookingRows").innerHTML = (D.bookings || []).map(b =>
    `<tr><td>${b.service || "Service"}</td><td>${fmtTs(b.scheduledAt)}</td><td>${statusSelect("bookings", b.id, b.status, BOOK_ST)}</td></tr>`).join("") || empty(3);

  document.getElementById("apptRows").innerHTML = (D.appointments || []).map(a =>
    `<tr><td>${a.purpose || "Appointment"}</td><td>${fmtTs(a.scheduledAt)}</td><td>${statusSelect("appointments", a.id, a.status, BOOK_ST)}</td></tr>`).join("") || empty(3);

  document.getElementById("reqRows").innerHTML = (D.serviceRequests || []).map(r =>
    `<tr><td>${r.title || "Request"}</td><td>${r.details || "-"}</td><td>${statusSelect("serviceRequests", r.id, r.status, REQ_ST)}</td></tr>`).join("") || empty(3);

  document.getElementById("reviewRows").innerHTML = (D.reviews || []).map(r =>
    `<tr><td>${r.customerName || "Customer"}</td><td>${star(r.rating)}</td><td>${r.comment || "-"}</td>
     <td><button class="reject" onclick="del('reviews','${r.id}')">Delete</button></td></tr>`).join("") || empty(4);

  const qc = filt("q-cust");
  document.getElementById("custRows").innerHTML = (D.users || []).filter(u => ((u.name || "") + (u.email || "")).toLowerCase().includes(qc)).map(u =>
    `<tr><td>${img(u.photoUrl)}</td><td>${u.name || "-"}</td><td>${u.email || "-"}</td>
     <td><select onchange="setRole('${u.id}',this.value)">${ROLES.map(r => opt(r, r, u.role || "CUSTOMER")).join("")}</select></td>
     <td><button class="mini" onclick="makeAdmin('${u.id}')">Make admin</button></td></tr>`).join("") || empty(5);

  document.getElementById("adminRows").innerHTML = (D.admins || []).map(a =>
    `<tr><td>${a.id}</td><td>${a.role || "admin"}</td><td><button class="reject" onclick="removeAdmin('${a.id}')">Remove</button></td></tr>`).join("") || empty(3);

  document.getElementById("growRows").innerHTML = (D.grow || []).map(g =>
    `<tr><td>${g.title}</td><td>${g.targetRole}</td><td>${g.ctaText || "-"}</td>
     <td class="row-actions"><button class="mini" onclick="editGrow('${g.id}')">Edit</button><button class="reject" onclick="del('growItems','${g.id}')">Delete</button></td></tr>`).join("") || empty(4);

  document.getElementById("bannerRows").innerHTML = (D.banners || []).map(b =>
    `<tr><td>${img(b.imageUrl)} ${b.title}</td><td><span class="tag info">${b.audience || "customer"}</span></td><td>${b.order ?? "-"}</td>
     <td>${b.active ? '<span class="tag ok">Yes</span>' : '<span class="tag no">No</span>'}</td>
     <td class="row-actions"><button class="mini" onclick="editBanner('${b.id}')">Edit</button>
     <button class="mini" onclick="toggleField('banners','${b.id}','active',${!b.active})">${b.active ? "Disable" : "Enable"}</button>
     <button class="reject" onclick="del('banners','${b.id}')">Delete</button></td></tr>`).join("") || empty(5);

  drawCharts();
}

function renderListing(col, rowsId, qId, availField, onLbl, offLbl) {
  const q = filt(qId);
  document.getElementById(rowsId).innerHTML = (DATA[col] || []).filter(s => (s.name || "").toLowerCase().includes(q)).map(s =>
    `<tr><td>${img(s.photoUrl)}</td><td>${s.name}${s.featured ? ' <span class="tag feat">★</span>' : ""}</td><td>${s.category || "-"}</td>
     <td>${star(s.rating)} ${(s.rating || 0).toFixed ? Number(s.rating || 0).toFixed(1) : s.rating}</td>
     <td>${s[availField] ? `<span class="tag ok">${onLbl}</span>` : `<span class="tag no">${offLbl}</span>`}</td>
     <td>${tag(s.approved)}</td>
     <td class="row-actions">
       <button class="mini" onclick="editListing('${col}','${s.id}')">Edit</button>
       ${s.approved ? `<button class="mini" onclick="toggleField('${col}','${s.id}','approved',false)">Unpublish</button>` : `<button class="approve" onclick="toggleField('${col}','${s.id}','approved',true)">Approve</button>`}
       <button class="reject" onclick="del('${col}','${s.id}')">Delete</button>
     </td></tr>`).join("") || empty(7);
}
function empty(c) { return `<tr><td colspan="${c}" class="empty">No data yet</td></tr>`; }

function drawCharts() {
  const cc = {}; [...(DATA.stores || []), ...(DATA.services || [])].forEach(s => { cc[s.category || "other"] = (cc[s.category || "other"] || 0) + 1; });
  const pal = ["#2563EB", "#7C3AED", "#F59E0B", "#22c55e", "#ef4444", "#06b6d4", "#ec4899", "#84cc16"];
  if (chartCat) chartCat.destroy();
  chartCat = new Chart(document.getElementById("chartCat"), { type: "doughnut", data: { labels: Object.keys(cc), datasets: [{ data: Object.values(cc), backgroundColor: pal }] }, options: { plugins: { legend: { labels: { color: "#93a4c0" }, position: "right" } } } });
  const st = ORDER_ST, counts = st.map(x => (DATA.orders || []).filter(o => (o.status || "").toUpperCase() === x).length);
  if (chartOrders) chartOrders.destroy();
  chartOrders = new Chart(document.getElementById("chartOrders"), { type: "bar", data: { labels: st, datasets: [{ data: counts, backgroundColor: pal, borderRadius: 6 }] }, options: { plugins: { legend: { display: false } }, scales: { x: { ticks: { color: "#93a4c0" }, grid: { display: false } }, y: { ticks: { color: "#93a4c0" }, grid: { color: "#22324f" } } } } });
}

/* ---------- generic actions ---------- */
async function setApprovalById(col, id, approved) { return toggleField(col, id, "approved", approved); }
async function toggleField(col, id, field, value) {
  if (!LIVE) { toast("(demo) updated", "ok"); return; }
  try { await db.collection(col).doc(id).update({ [field]: value }); toast("Updated", "ok"); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
async function changeStatus(col, id, value) {
  if (!LIVE) { toast("(demo) status set", "ok"); return; }
  try { await db.collection(col).doc(id).update({ status: value }); toast("Status updated", "ok"); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
async function del(col, id) {
  if (!confirm("Delete this item?")) return;
  if (!LIVE) { toast("(demo) deleted", "ok"); return; }
  try { await db.collection(col).doc(id).delete(); toast("Deleted", "ok"); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}

/* ---------- edit store / service ---------- */
function editListing(col, id) {
  const s = (DATA[col] || []).find(x => x.id === id); if (!s) return;
  const isStore = col === "stores"; const availField = isStore ? "isOpen" : "available";
  modal(`<h3>Edit ${isStore ? "store" : "provider"}</h3>
    <label>Name</label><input id="e-name" value="${esc(s.name)}">
    <label>Category</label><input id="e-cat" value="${esc(s.category)}">
    <label>Address / area</label><input id="e-addr" value="${esc(s.address)}">
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
      <div><label>Approved</label><select id="e-appr">${opt("true", "Yes", String(!!s.approved))}${opt("false", "No", String(!!s.approved))}</select></div>
      <div><label>${isStore ? "Open" : "Available"}</label><select id="e-avail">${opt("true", "Yes", String(!!s[availField]))}${opt("false", "No", String(!!s[availField]))}</select></div>
      <div><label>Featured</label><select id="e-feat">${opt("false", "No", String(!!s.featured))}${opt("true", "Yes", String(!!s.featured))}</select></div>
    </div>
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" onclick="saveListing('${col}','${id}','${availField}')">Save</button></div>`);
}
async function saveListing(col, id, availField) {
  const data = { name: val("e-name"), category: val("e-cat"), address: val("e-addr"),
    approved: val("e-appr") === "true", [availField]: val("e-avail") === "true", featured: val("e-feat") === "true" };
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  try { await db.collection(col).doc(id).update(data); toast("Saved", "ok"); closeModal(); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}

/* ---------- categories ---------- */
function openCategory() {
  modal(`<h3>Add category</h3>
    <label>Name</label><input id="c-name" placeholder="e.g. groceries">
    <label>Type</label><select id="c-type">${opt("store", "Store", "store")}${opt("service", "Service", "store")}</select>
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" onclick="saveCategory()">Save</button></div>`);
}
async function saveCategory() {
  const name = val("c-name"); if (!name) return toast("Name required", "bad");
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  try { await db.collection("categories").add({ name, type: val("c-type"), iconUrl: "" }); toast("Category added", "ok"); closeModal(); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}

/* ---------- master catalog ---------- */
let editCatalogId = null;
function editCatalogItem(id) { openCatalogItem((DATA.catalog || []).find(x => x.id === id)); }
function openCatalogItem(c) {
  editCatalogId = c?.id || null; const type = c?.type || "product";
  modal(`<h3>${c ? "Edit" : "Add"} catalog item</h3>
    <label>Name</label><input id="ci-name" value="${esc(c?.name)}" placeholder="e.g. Rice 5kg">
    <label>Type</label><select id="ci-type">${opt("product", "Product", type)}${opt("service", "Service", type)}</select>
    <label>Category</label><input id="ci-cat" value="${esc(c?.category)}" placeholder="e.g. groceries / plumber">
    <label>Unit (optional)</label><input id="ci-unit" value="${esc(c?.unit)}" placeholder="e.g. 5 kg, per visit">
    <label>Suggested MRP (₹)</label><input id="ci-mrp" type="number" value="${c?.suggestedMrp ?? ""}">
    <label>Description (optional)</label><input id="ci-desc" value="${esc(c?.description)}">
    <label>Image (optional)</label><input id="ci-file" type="file" accept="image/*" onchange="previewFile('ci-file','ci-prev')">
    <img id="ci-prev" src="${c?.imageUrl || ""}" style="display:${c?.imageUrl ? "block" : "none"};width:100%;height:120px;object-fit:cover;border-radius:10px;margin-top:8px">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" id="ci-save" onclick="saveCatalogItem()">Save</button></div>`);
}
async function saveCatalogItem() {
  const name = val("ci-name"); if (!name) return toast("Name required", "bad");
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  const btn = document.getElementById("ci-save"); if (btn) { btn.disabled = true; btn.textContent = "Saving…"; }
  try {
    const imageUrl = await uploadImage("ci-file", "catalog");
    const data = { name, type: val("ci-type"), category: val("ci-cat"), unit: val("ci-unit"),
      suggestedMrp: parseFloat(val("ci-mrp") || "0") || 0, description: val("ci-desc") };
    if (imageUrl) data.imageUrl = imageUrl;
    if (editCatalogId) await db.collection("catalog").doc(editCatalogId).update(data);
    else await db.collection("catalog").add({ ...data, imageUrl: imageUrl || "" });
    toast("Catalog item saved", "ok"); closeModal(); await loadAll();
  } catch (e) { toast("Failed: " + e.message, "bad"); if (btn) { btn.disabled = false; btn.textContent = "Save"; } }
}

/* ---------- customers / admins ---------- */
async function setRole(uid, role) {
  if (!LIVE) { toast("(demo) role set", "ok"); return; }
  try { await db.collection("users").doc(uid).update({ role }); toast("Role updated", "ok"); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
async function makeAdmin(uid) {
  if (!confirm("Grant admin access to this user?")) return;
  if (!LIVE) { toast("(demo) admin added", "ok"); return; }
  try { await db.collection("admins").doc(uid).set({ role: "admin" }); toast("Admin granted", "ok"); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
function openAddAdmin() {
  modal(`<h3>Add admin</h3><label>User UID</label><input id="a-uid" placeholder="paste UID from Authentication → Users">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" onclick="addAdmin()">Add</button></div>`);
}
async function addAdmin() {
  const uid = val("a-uid"); if (!uid) return toast("UID required", "bad");
  if (!LIVE) { toast("(demo) added", "ok"); return closeModal(); }
  try { await db.collection("admins").doc(uid).set({ role: "admin" }); toast("Admin added", "ok"); closeModal(); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}
async function removeAdmin(uid) {
  if (!confirm("Remove this admin?")) return;
  if (!LIVE) { toast("(demo) removed", "ok"); return; }
  try { await db.collection("admins").doc(uid).delete(); toast("Admin removed", "ok"); await loadAll(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}

/* ---------- notifications ---------- */
async function sendNotification() {
  const title = val("n-title"), body = val("n-body"), aud = val("n-aud");
  if (!title) return toast("Title required", "bad");
  if (!LIVE) { toast("(demo) sent", "ok"); return; }
  try {
    let targets = [];
    if (aud === "uid") { const u = val("n-uid"); if (!u) return toast("UID required", "bad"); targets = [u]; }
    else {
      const users = DATA.users || [];
      const sellerRoles = ["STORE_OWNER", "SERVICE_PROVIDER", "STORE_AND_PROVIDER"];
      targets = users.filter(u => aud === "all" || (aud === "customer" && (u.role || "CUSTOMER") === "CUSTOMER") || (aud === "seller" && sellerRoles.includes(u.role))).map(u => u.id);
    }
    if (!targets.length) return toast("No recipients", "bad");
    let batch = db.batch(), n = 0;
    for (const uid of targets) {
      const ref = db.collection("notifications").doc();
      batch.set(ref, { toUid: uid, title, body, read: false, createdAt: firebase.firestore.FieldValue.serverTimestamp() });
      if (++n % 400 === 0) { await batch.commit(); batch = db.batch(); }
    }
    await batch.commit();
    toast(`Sent to ${targets.length} user(s)`, "ok");
    document.getElementById("n-title").value = ""; document.getElementById("n-body").value = "";
  } catch (e) { toast("Failed: " + e.message, "bad"); }
}

/* ---------- image upload ---------- */
async function uploadImage(fileInputId, folder) {
  const f = document.getElementById(fileInputId)?.files?.[0];
  if (!f || !LIVE) return "";
  const ref = storage.ref().child(`${folder}/${Date.now()}_${f.name.replace(/\s+/g, "_")}`);
  await ref.put(f); return await ref.getDownloadURL();
}
function previewFile(inputId, imgId) { const f = document.getElementById(inputId)?.files?.[0]; const im = document.getElementById(imgId); if (f && im) { im.src = URL.createObjectURL(f); im.style.display = "block"; } }

/* ---------- grow items ---------- */
let editGrowId = null;
function editGrow(id) { openGrow((DATA.grow || []).find(x => x.id === id)); }
function openGrow(g) {
  editGrowId = g?.id || null; const role = g?.targetRole || "all";
  modal(`<h3>${g ? "Edit" : "Add"} grow item</h3>
    <label>Title</label><input id="g-title" value="${esc(g?.title)}">
    <label>Description</label><input id="g-desc" value="${esc(g?.description)}">
    <label>CTA text</label><input id="g-cta" value="${esc(g?.ctaText)}">
    <label>Target audience</label><select id="g-role">${opt("all", "All sellers", role)}${opt("store_owner", "Store owners", role)}${opt("service_provider", "Service providers", role)}</select>
    <label>Image (optional)</label><input id="g-file" type="file" accept="image/*" onchange="previewFile('g-file','g-prev')">
    <img id="g-prev" src="${g?.imageUrl || ""}" style="display:${g?.imageUrl ? "block" : "none"};width:100%;height:120px;object-fit:cover;border-radius:10px;margin-top:8px">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" id="g-save" onclick="saveGrow()">Save</button></div>`);
}
async function saveGrow() {
  const title = val("g-title"); if (!title) return toast("Title required", "bad");
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  const btn = document.getElementById("g-save"); if (btn) { btn.disabled = true; btn.textContent = "Saving…"; }
  try {
    const imageUrl = await uploadImage("g-file", "growItems");
    const data = { title, description: val("g-desc"), ctaText: val("g-cta"), targetRole: val("g-role") };
    if (imageUrl) data.imageUrl = imageUrl;
    if (editGrowId) await db.collection("growItems").doc(editGrowId).update(data);
    else await db.collection("growItems").add({ ...data, imageUrl: imageUrl || "" });
    toast("Saved", "ok"); closeModal(); await loadAll();
  } catch (e) { toast("Failed: " + e.message, "bad"); if (btn) { btn.disabled = false; btn.textContent = "Save"; } }
}

/* ---------- banners ---------- */
let editBannerId = null;
function editBanner(id) { openBanner((DATA.banners || []).find(x => x.id === id)); }
function openBanner(b) {
  editBannerId = b?.id || null; const aud = b?.audience || "customer";
  modal(`<h3>${b ? "Edit" : "Add"} banner</h3>
    <label>Title</label><input id="b-title" value="${esc(b?.title)}">
    <label>Show in app</label><select id="b-aud">${opt("customer", "Customer app", aud)}${opt("seller", "Seller app", aud)}${opt("both", "Both apps", aud)}</select>
    <label>Banner image ${b ? "(leave empty to keep current)" : ""}</label>
    <input id="b-file" type="file" accept="image/*" onchange="previewFile('b-file','b-prev')">
    <img id="b-prev" src="${b?.imageUrl || ""}" style="display:${b?.imageUrl ? "block" : "none"};width:100%;height:120px;object-fit:cover;border-radius:10px;margin-top:8px">
    <label>Order</label><input id="b-order" type="number" value="${b?.order ?? 1}">
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px">
      <div><label>Corner</label><select id="b-corner">${opt("default", "Default", b?.cornerStyle || "default")}${opt("rounded", "Rounded", b?.cornerStyle || "default")}${opt("square", "Square", b?.cornerStyle || "default")}</select></div>
      <div><label>Border</label><select id="b-border">${opt("default", "Default", b?.borderStyle || "default")}${opt("on", "On", b?.borderStyle || "default")}${opt("off", "Off", b?.borderStyle || "default")}</select></div>
      <div><label>Height</label><select id="b-height">${opt("default", "Default", b?.heightStyle || "default")}${opt("short", "Short", b?.heightStyle || "default")}${opt("medium", "Medium", b?.heightStyle || "default")}${opt("tall", "Tall", b?.heightStyle || "default")}</select></div>
      <div><label>Template</label><select id="b-template">${opt("default", "Default", b?.template || "default")}${opt("full", "Full image", b?.template || "default")}${opt("overlay", "Image + title", b?.template || "default")}${opt("framed", "Framed", b?.template || "default")}</select></div>
    </div>
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" id="b-save" onclick="saveBanner()">Save</button></div>`);
}
async function saveBanner() {
  const title = val("b-title"); if (!title) return toast("Title required", "bad");
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  const btn = document.getElementById("b-save"); if (btn) { btn.disabled = true; btn.textContent = "Saving…"; }
  try {
    const imageUrl = await uploadImage("b-file", "banners");
    const data = { title, audience: val("b-aud"), order: parseInt(val("b-order") || "1", 10),
      cornerStyle: val("b-corner"), borderStyle: val("b-border"), heightStyle: val("b-height"), template: val("b-template") };
    if (imageUrl) data.imageUrl = imageUrl;
    if (editBannerId) await db.collection("banners").doc(editBannerId).update(data);
    else await db.collection("banners").add({ ...data, imageUrl: imageUrl || "", active: true });
    toast("Saved", "ok"); closeModal(); await loadAll();
  } catch (e) { toast("Failed: " + e.message, "bad"); if (btn) { btn.disabled = false; btn.textContent = "Save"; } }
}
function openBannerSettings() {
  const s = BSETTINGS;
  modal(`<h3>Global banner design</h3><p style="color:var(--muted);font-size:13px;margin-bottom:6px">Applies to banners set to "Default".</p>
    <label>Corner</label><select id="s-corner">${opt("rounded", "Rounded", s.cornerStyle)}${opt("square", "Square", s.cornerStyle)}</select>
    <label>Border</label><select id="s-border">${opt("false", "Off", String(s.border))}${opt("true", "On", String(s.border))}</select>
    <label>Height</label><select id="s-height">${opt("short", "Short", s.heightStyle)}${opt("medium", "Medium", s.heightStyle)}${opt("tall", "Tall", s.heightStyle)}</select>
    <label>Template</label><select id="s-template">${opt("full", "Full image", s.template)}${opt("overlay", "Image + title", s.template)}${opt("framed", "Framed", s.template)}</select>
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="add" onclick="saveBannerSettings()">Save</button></div>`);
}
async function saveBannerSettings() {
  const data = { cornerStyle: val("s-corner"), border: val("s-border") === "true", heightStyle: val("s-height"), template: val("s-template") };
  if (!LIVE) { toast("(demo) saved", "ok"); return closeModal(); }
  try { await db.collection("settings").doc("banners").set(data); BSETTINGS = data; toast("Banner design saved", "ok"); closeModal(); }
  catch (e) { toast("Failed: " + e.message, "bad"); }
}

if (LIVE) auth.onAuthStateChanged(async u => { if (u && await checkAdmin(u.uid)) showApp(u); });
