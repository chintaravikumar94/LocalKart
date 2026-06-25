/* LocalKart — Customer web app. Same Firebase project as the Android apps. */
const firebaseConfig = {
  apiKey: "AIzaSyC4h_btIhf5cGkMFm2N92j8RsdBfNt7DZY",
  authDomain: "localkart-7dfb4.firebaseapp.com",
  projectId: "localkart-7dfb4",
  storageBucket: "localkart-7dfb4.firebasestorage.app",
  messagingSenderId: "537191050226",
  appId: "1:537191050226:web:f4215514741ee93cbb196b"
};
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth(), db = firebase.firestore(), storage = firebase.storage();
const FV = firebase.firestore.FieldValue, TS = firebase.firestore.Timestamp;

/* ---------- state ---------- */
let ME = null;                 // user doc {uid,name,email,phone,address,photoUrl}
const DATA = { stores: [], services: [], categories: [], banners: [] };
let CART = [];                 // [{productId,name,imageUrl,price,qty,storeId,storeName}]
let CUR = null;                // current detail target {kind:'store'|'service', item}
let LOC = null;                // {lat,lng,label}
let SIGNUP = false;
let chatUnsub = null, curThreadId = null;

/* ---------- theme ---------- */
function applyTheme(t){ document.body.classList.toggle("dark", t === "dark"); }
function toggleTheme(){ const n = document.body.classList.contains("dark") ? "light" : "dark"; try{localStorage.setItem("lk-theme",n)}catch(e){} applyTheme(n); }
applyTheme((()=>{try{return localStorage.getItem("lk-theme")}catch(e){return null}})() || "light");

/* ---------- helpers ---------- */
const $ = id => document.getElementById(id);
function toast(m,k){ const t=document.createElement("div"); t.className="toast "+(k||""); t.textContent=m; $("toast").appendChild(t); setTimeout(()=>t.remove(),3000); }
function modal(html){ $("modalBox").innerHTML=html; $("modalBg").classList.add("open"); }
function closeModal(){ $("modalBg").classList.remove("open"); }
$("modalBg").addEventListener("click",e=>{ if(e.target.id==="modalBg") closeModal(); });
const num = n => (n||0).toLocaleString("en-IN");
const money = n => "₹"+num(Math.round(n||0));
const esc = s => (s==null?"":String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c])));
const catLabel = n => (n||"").replace(/_/g," ").replace(/\b\w/g,c=>c.toUpperCase());
const star = r => '<span class="stars">'+"★".repeat(Math.round(r||0)).padEnd(5,"☆")+"</span>";
const img = (u,cls) => u ? `<img class="${cls||""}" src="${esc(u)}">` : `<div class="${cls||""}" style="display:grid;place-items:center;font-size:22px;color:var(--muted)">🏬</div>`;
function fmtTs(ts){ if(!ts) return "-"; const ms = ts.seconds?ts.seconds*1000:(ts.toMillis?ts.toMillis():ts._seconds*1000); return new Date(ms).toLocaleString("en-IN",{dateStyle:"medium",timeStyle:"short"}); }
function tsMs(ts){ return ts? (ts.seconds?ts.seconds*1000:(ts.toMillis?ts.toMillis():0)):0; }
function distKm(a,b){ if(!a||!b) return null; const R=6371,dLat=(b.lat-a.lat)*Math.PI/180,dLng=(b.lng-a.lng)*Math.PI/180;
  const x=Math.sin(dLat/2)**2+Math.cos(a.lat*Math.PI/180)*Math.cos(b.lat*Math.PI/180)*Math.sin(dLng/2)**2; return R*2*Math.atan2(Math.sqrt(x),Math.sqrt(1-x)); }
function geo(item){ const g=item.location; if(g&&typeof g.latitude==="number") return {lat:g.latitude,lng:g.longitude}; return null; }
function distLabel(item){ const d=LOC&&geo(item)?distKm(LOC,geo(item)):null; return d!=null?` · ${d.toFixed(1)} km`:""; }

/* ---------- auth ---------- */
function loginErr(m){ $("loginErr").textContent=m||""; }
function toggleMode(){ SIGNUP=!SIGNUP; $("signupName").style.display=SIGNUP?"block":"none"; $("loginBtn").textContent=SIGNUP?"Create account":"Sign in"; $("toggleMode").textContent=SIGNUP?"Have an account? Sign in":"New here? Create an account"; }
async function loginGoogle(){ loginErr(""); try{ const c=await auth.signInWithPopup(new firebase.auth.GoogleAuthProvider()); await afterLogin(c.user); }catch(e){ loginErr(e.message); } }
async function loginEmail(){
  loginErr(""); const email=$("email").value.trim(), pass=$("password").value;
  if(!email||!pass) return loginErr("Enter email and password.");
  try{
    let c;
    if(SIGNUP){ c=await auth.createUserWithEmailAndPassword(email,pass); await c.user.updateProfile({displayName:$("name").value.trim()||email.split("@")[0]}); }
    else c=await auth.signInWithEmailAndPassword(email,pass);
    await afterLogin(c.user);
  }catch(e){ loginErr(e.message); }
}
async function ensureUserDoc(u){
  const ref=db.collection("users").doc(u.uid); const snap=await ref.get();
  if(!snap.exists){ const d={name:u.displayName||"",email:u.email||"",phone:"",photoUrl:u.photoURL||"",role:"CUSTOMER",address:"",createdAt:FV.serverTimestamp()}; await ref.set(d); return {uid:u.uid,...d}; }
  return {uid:u.uid,...snap.data()};
}
async function afterLogin(u){ ME=await ensureUserDoc(u); startApp(); }
function logout(){ auth.signOut(); location.reload(); }
auth.onAuthStateChanged(async u=>{ if(u){ try{ ME=await ensureUserDoc(u); startApp(); }catch(e){ loginErr(e.message); } } });

function startApp(){
  $("login").style.display="none"; $("app").style.display="flex";
  $("meAv").textContent=(ME.name||ME.email||"U")[0].toUpperCase();
  try{ CART=JSON.parse(localStorage.getItem("lk-cart")||"[]"); }catch(e){ CART=[]; }
  try{ LOC=JSON.parse(localStorage.getItem("lk-loc")||"null"); }catch(e){ LOC=null; }
  $("locLabel").textContent=LOC?.label || "Set location";
  cartBadge();
  loadAll();
}

/* ---------- nav ---------- */
$("tabs").addEventListener("click",e=>{ const a=e.target.closest("a"); if(a) go(a.dataset.view); });
function go(view){
  document.querySelectorAll("#tabs a").forEach(x=>x.classList.toggle("active",x.dataset.view===view));
  document.querySelectorAll(".view").forEach(v=>v.classList.remove("active"));
  $("view-"+view).classList.add("active");
  if(chatUnsub && view!=="chat"){ chatUnsub(); chatUnsub=null; }
  if(view!=="home") stopCarousel();   // stop banner auto-rotate when leaving Home
  window.scrollTo(0,0);
  ({home:renderHome,stores:renderStores,services:renderServices,cart:renderCart,orders:renderOrders,bookings:renderBookings,chat:renderChat,notifications:renderNotifications,account:renderAccount}[view]||(()=>{}))();
}

/* ---------- data ---------- */
async function loadAll(){
  try{
    const [st,sv,cat,bn] = await Promise.all([
      db.collection("stores").where("approved","==",true).get(),
      db.collection("services").where("approved","==",true).get(),
      db.collection("categories").get(),
      db.collection("banners").where("active","==",true).get()
    ]);
    DATA.stores = st.docs.map(d=>({id:d.id,...d.data()}));
    DATA.services = sv.docs.map(d=>({id:d.id,...d.data()}));
    DATA.categories = cat.docs.map(d=>({id:d.id,...d.data()}));
    DATA.banners = bn.docs.map(d=>({id:d.id,...d.data()})).filter(b=>["customer","both"].includes(b.audience||"customer")).sort((a,b)=>(a.order||0)-(b.order||0));
  }catch(e){ toast("Load failed: "+e.message,"bad"); }
  loadNotifBadge();
  renderHome();
}

/* ---------- home ---------- */
function renderHome(){
  const cats = DATA.categories;
  const storeCats = cats.filter(c=>(c.type||"store")==="store");
  const svcCats = cats.filter(c=>c.type==="service");
  const nearby = [...DATA.stores].sort((a,b)=>{ const da=LOC&&geo(a)?distKm(LOC,geo(a)):9e9, dbb=LOC&&geo(b)?distKm(LOC,geo(b)):9e9; return da-dbb; });
  $("view-home").innerHTML = `
    ${bannerHtml()}
    <h2 class="sec">Shop by store category</h2>
    <div class="chips">${storeCats.map(c=>chipHtml(c,"stores")).join("")||emptyInline("No categories yet")}</div>
    <h2 class="sec">Book a service</h2>
    <div class="chips">${svcCats.map(c=>chipHtml(c,"services")).join("")||emptyInline("No services yet")}</div>
    <h2 class="sec">Stores near you <span class="crumb">${DATA.stores.length} open</span></h2>
    <div class="grid">${nearby.slice(0,8).map(s=>storeCardHtml(s,"store")).join("")||emptyInline("No stores yet")}</div>
    <h2 class="sec">Popular services</h2>
    <div class="grid">${DATA.services.slice(0,8).map(s=>storeCardHtml(s,"service")).join("")||emptyInline("No providers yet")}</div>
  `;
  startCarousel();
}
function emptyInline(t){ return `<div class="empty" style="grid-column:1/-1">${t}</div>`; }
function chipHtml(c,view){ const e=catEmoji(c.name); return `<div class="chip" onclick="go('${view}');setTimeout(()=>filterCat('${view}','${esc(c.name)}'),60)">
  <div class="ci">${c.iconUrl?`<img src="${esc(c.iconUrl)}">`:e}</div><small>${catLabel(c.name)}</small></div>`; }
function catEmoji(n){ const m={groceries:"🛒",vegetables_fruits:"🥦",bakery:"🥖",sweets:"🍬",dairy:"🥛",meat_fish:"🍗",medical_pharmacy:"💊",mobile_repairing:"📱",electronics:"🔌",hardware:"🔩",stationery:"✏️",clothing:"👕",footwear:"👟",jewellery:"💍",furniture:"🪑",restaurant:"🍽️",household:"🧹",plumber:"🚰",electrician:"💡",carpenter:"🪚",painter:"🎨",mechanic:"🔧",ac_repair:"❄️",housekeeping:"🧽",cook:"👨‍🍳",salon_spa:"💇",tutor:"📚",photographer:"📷"}; return m[n]||"🏷️"; }
function storeCardHtml(s,kind){
  const openTag = kind==="store" ? (s.isOpen?'<span class="pill open">Open</span>':'<span class="pill closed">Closed</span>') : (s.available?'<span class="pill open">Available</span>':'<span class="pill closed">Busy</span>');
  return `<div class="card" onclick="openDetail('${kind}','${s.id}')">
    <div class="ph">${img(s.photoUrl)}${openTag}</div>
    <div class="bd"><div class="nm">${esc(s.name)}</div>
      <div class="between"><span class="crumb">${catLabel(s.category)}</span><span class="rate">★ ${(s.rating||0).toFixed(1)}</span></div>
      <div class="crumb" style="margin-top:3px">${esc(s.address||"")}${distLabel(s)}</div>
    </div></div>`;
}

/* ---------- banners carousel ---------- */
let carIdx=0, carTimer=null;
function bannerHtml(){
  if(!DATA.banners.length) return "";
  return `<div class="carousel"><div class="track" id="bTrack">${DATA.banners.map(b=>`
    <div class="slide">${img(b.imageUrl)}${b.title?`<div class="cap">${esc(b.title)}</div>`:""}</div>`).join("")}</div></div>
    <div class="dots" id="bDots">${DATA.banners.map((_,i)=>`<span class="${i===0?"on":""}"></span>`).join("")}</div>`;
}
function stopCarousel(){ if(carTimer){ clearInterval(carTimer); carTimer=null; } }
function startCarousel(){
  stopCarousel(); carIdx=0;
  if(DATA.banners.length<=1) return;
  carTimer=setInterval(()=>{
    const t=$("bTrack"); const dots=$("bDots");
    if(!t || !dots || !$("view-home").classList.contains("active")){ stopCarousel(); return; }  // only run on Home
    carIdx=(carIdx+1)%DATA.banners.length;
    t.style.transform=`translateX(-${carIdx*100}%)`;
    for(let i=0;i<dots.children.length;i++) dots.children[i].className=i===carIdx?"on":"";
  },3500);
}

/* ---------- stores / services lists ---------- */
let filterState={stores:"all",services:"all"};
function filterCat(view,cat){ filterState[view]=cat; view==="stores"?renderStores():renderServices(); }
function listView(view){
  const isStore=view==="stores"; const items=isStore?DATA.stores:DATA.services;
  const cats=DATA.categories.filter(c=>(c.type||"store")===(isStore?"store":"service"));
  const sel=filterState[view];
  const q=($("globalSearch").value||"").toLowerCase();
  let list=items.filter(s=>(sel==="all"||s.category===sel)&&(s.name||"").toLowerCase().includes(q));
  if(LOC) list.sort((a,b)=>{ const da=geo(a)?distKm(LOC,geo(a)):9e9,dbb=geo(b)?distKm(LOC,geo(b)):9e9; return da-dbb; });
  const chips=[`<div class="chip ${sel==="all"?"on":""}" onclick="filterCat('${view}','all')"><div class="ci">🟦</div><small>All</small></div>`]
    .concat(cats.map(c=>`<div class="chip ${sel===c.name?"on":""}" onclick="filterCat('${view}','${esc(c.name)}')"><div class="ci">${c.iconUrl?`<img src="${esc(c.iconUrl)}">`:catEmoji(c.name)}</div><small>${catLabel(c.name)}</small></div>`)).join("");
  $("view-"+view).innerHTML=`<h2 class="sec">${isStore?"Stores":"Service providers"} <span class="crumb">${list.length} found</span></h2>
    <div class="chips">${chips}</div>
    <div class="grid">${list.map(s=>storeCardHtml(s,isStore?"store":"service")).join("")||emptyInline("Nothing here yet")}</div>`;
}
function renderStores(){ listView("stores"); }
function renderServices(){ listView("services"); }

/* ---------- detail ---------- */
async function openDetail(kind,id){
  const item=(kind==="store"?DATA.stores:DATA.services).find(x=>x.id===id);
  if(!item) return toast("Not found","bad");
  CUR={kind,item}; go("detail");                       // go() stops the banner carousel
  $("view-detail").innerHTML=`<div class="empty">Loading ${esc(item.name)}…</div>`;
  let extras=[];
  try{
    if(kind==="store") extras=(await db.collection("products").where("storeId","==",id).get()).docs.map(d=>({id:d.id,...d.data()})).filter(p=>p.approved);
    else extras=(await db.collection("serviceOfferings").where("providerId","==",id).get()).docs.map(d=>({id:d.id,...d.data()})).filter(o=>o.approved);
  }catch(e){ /* offerings may use ownerUid; ignore */ }
  try{ renderDetail(item,kind,extras); }
  catch(e){ $("view-detail").innerHTML=`<div class="empty">Couldn't open this shop.<br><button class="btn" style="margin-top:10px" onclick="go('${kind==="store"?"stores":"services"}')">← Back</button></div>`; }
}
function renderDetail(s,kind,extras){
  const isStore=kind==="store";
  const head=`<div class="panel" style="padding:0;overflow:hidden">
      <div class="hero">${s.photoUrl?`<img src="${esc(s.photoUrl)}" alt="">`:`<div class="hero-ph">🏬</div>`}</div>
      <div style="padding:16px">
        <div class="between"><h2 style="font-size:20px">${esc(s.name)}</h2>
          <span class="rate">★ ${(s.rating||0).toFixed(1)} <span class="crumb">(${s.ratingCount||0})</span></span></div>
        <div class="crumb" style="margin-top:4px">${catLabel(s.category)} · ${isStore?(s.isOpen?'<span class="tag ok">Open now</span>':'<span class="tag no">Closed</span>'):(s.available?'<span class="tag ok">Available</span>':'<span class="tag no">Busy</span>')}</div>
        <div class="crumb" style="margin-top:4px">${esc(s.address||"")}${distLabel(s)}</div>
        ${s.description?`<div style="margin-top:6px;font-size:14px">${esc(s.description)}</div>`:""}
      <div class="row" style="margin-top:12px;flex-wrap:wrap">
        <button class="ghost" onclick="startChat()">💬 Chat</button>
        <button class="ghost" onclick="openReviews()">⭐ Reviews</button>
        ${isStore?`<button class="ghost" onclick="openAppointment()">🗓️ Book appointment</button>`
                 :`<button class="ghost" onclick="openBooking()">📅 Book service</button><button class="ghost" onclick="openRequest()">🛠️ Request job</button>`}
        <button class="btn alt" onclick="go('${isStore?"stores":"services"}')">← Back</button>
      </div></div></div>`;
  let body="";
  if(isStore){
    body=`<h2 class="sec">Products <span class="crumb">${extras.length} items</span></h2>
      <div class="pgrid">${extras.map(p=>productTile(p,s)).join("")||emptyInline("No products listed yet")}</div>`;
  }else{
    body=`<h2 class="sec">Services offered <span class="crumb">${extras.length}</span></h2>
      <div class="pgrid">${extras.map(o=>offeringTile(o,s)).join("")||emptyInline("Use “Book service” or “Request job” above.")}</div>`;
  }
  $("view-detail").innerHTML=head+contactBlock(s)+body;
}
function waLink(n){ let d=(n||"").replace(/\D/g,""); if(d.length===10) d="91"+d; return "https://wa.me/"+d; }
function contactBlock(s){
  if(!s.showContact) return "";
  const phone=(s.phone||"").trim(), wa=(s.whatsapp||"").trim(), g=geo(s);
  let btns="";
  if(phone) btns+=`<a class="ghost" href="tel:${esc(phone)}">📞 Call</a>`;
  if(wa) btns+=`<a class="ghost" href="${waLink(wa)}" target="_blank" rel="noopener">🟢 WhatsApp</a>`;
  if(g) btns+=`<a class="ghost" href="https://www.google.com/maps/search/?api=1&query=${g.lat},${g.lng}" target="_blank" rel="noopener">🗺️ Directions</a>`;
  if(!btns && !s.ownerPhotoUrl) return "";
  const sub=[phone?esc(phone):"", (wa&&wa!==phone)?"WhatsApp: "+esc(wa):""].filter(Boolean).join(" · ");
  return `<div class="panel"><div class="row">
      ${s.ownerPhotoUrl?`<img src="${esc(s.ownerPhotoUrl)}" style="width:58px;height:58px;border-radius:50%;object-fit:cover">`:`<div style="width:58px;height:58px;border-radius:50%;background:var(--card-2);display:grid;place-items:center;font-size:24px">👤</div>`}
      <div style="flex:1"><b>Contact the owner</b>${sub?`<div class="crumb" style="margin-top:2px">${sub}</div>`:""}</div>
    </div>
    <div class="row" style="margin-top:10px;flex-wrap:wrap">${btns}</div></div>`;
}
function priceBlock(price,mrp){
  let h=`<div><span class="price">${money(price)}</span>${mrp>price?`<span class="mrp">${money(mrp)}</span>`:""}</div>`;
  if(mrp>price){ const save=Math.round(mrp-price), pct=Math.round((mrp-price)/mrp*100); h+=`<div class="save">You save ${money(save)} (${pct}% off)</div>`; }
  return h;
}
function productTile(p,store){
  const inCart=CART.find(x=>x.productId===p.id);
  return `<div class="ptile"><div class="ph">${img(p.imageUrl)}</div><div class="bd">
    <div class="nm">${esc(p.name)}</div>${p.unit?`<div class="crumb">${esc(p.unit)}</div>`:""}
    ${priceBlock(p.price,p.mrp)}
    ${!p.inStock?`<button class="addbtn" disabled>Out of stock</button>`
      : inCart?qtyControl(p.id):`<button class="addbtn" onclick='addToCart(${JSON.stringify({id:p.id,name:p.name,imageUrl:p.imageUrl||"",price:p.price,storeId:store.id,storeName:store.name}).replace(/'/g,"&#39;")})'>Add to cart</button>`}
  </div></div>`;
}
function offeringTile(o,prov){
  return `<div class="ptile"><div class="ph">${img(o.imageUrl)}</div><div class="bd">
    <div class="nm">${esc(o.name)}</div>${priceBlock(o.price,o.mrp)}
    <button class="addbtn" onclick="openBooking('${esc(o.name)}')">Book this</button></div></div>`;
}
function qtyControl(pid){ const it=CART.find(x=>x.productId===pid); const q=it?it.qty:0;
  return `<div class="qtybox"><button onclick="chgQty('${pid}',-1)">−</button><b>${q}</b><button onclick="chgQty('${pid}',1)">＋</button></div>`; }

/* ---------- cart ---------- */
function saveCart(){ try{localStorage.setItem("lk-cart",JSON.stringify(CART));}catch(e){} cartBadge(); }
function cartBadge(){ const n=CART.reduce((s,i)=>s+i.qty,0); const b=$("cartBadge"); b.style.display=n?"grid":"none"; b.textContent=n; }
function addToCart(p){
  if(CART.length && CART[0].storeId!==p.storeId){ if(!confirm("Your cart has items from another store. Clear it and add this?")) return; CART=[]; }
  CART.push({productId:p.id,name:p.name,imageUrl:p.imageUrl,price:p.price,qty:1,storeId:p.storeId,storeName:p.storeName});
  saveCart(); toast("Added to cart","ok"); if($("view-detail").classList.contains("active")&&CUR) openDetail(CUR.kind,CUR.item.id);
}
function chgQty(pid,d){ const it=CART.find(x=>x.productId===pid); if(!it) return; it.qty+=d; if(it.qty<=0) CART=CART.filter(x=>x.productId!==pid); saveCart();
  if($("view-cart").classList.contains("active")) renderCart(); else if(CUR) openDetail(CUR.kind,CUR.item.id); }
function renderCart(){
  if(!CART.length){ $("view-cart").innerHTML=`<h2 class="sec">Cart</h2><div class="empty">🛒 Your cart is empty.<br><button class="btn" style="margin-top:12px" onclick="go('stores')">Browse stores</button></div>`; return; }
  const total=CART.reduce((s,i)=>s+i.price*i.qty,0);
  $("view-cart").innerHTML=`<h2 class="sec">Cart <span class="crumb">${CART[0].storeName||""}</span></h2>
    <div class="panel">${CART.map(i=>`<div class="listrow">
      ${img(i.imageUrl,"thumb")}
      <div style="flex:1"><b>${esc(i.name)}</b><div class="crumb">${money(i.price)} each</div></div>
      <div class="qtybox"><button onclick="chgQty('${i.productId}',-1)">−</button><b>${i.qty}</b><button onclick="chgQty('${i.productId}',1)">＋</button></div>
      <b style="width:80px;text-align:right">${money(i.price*i.qty)}</b>
    </div>`).join("")}
      <div class="between" style="margin-top:14px"><b style="font-size:17px">Total</b><b style="font-size:17px">${money(total)}</b></div>
      <button class="btn block" style="margin-top:14px" onclick="checkout()">Place order · ${money(total)}</button>
      <button class="btn alt block" style="margin-top:8px" onclick="CART=[];saveCart();renderCart()">Clear cart</button>
    </div>`;
}
async function checkout(){
  if(!CART.length) return;
  const total=CART.reduce((s,i)=>s+i.price*i.qty,0);
  const items=CART.map(i=>({productId:i.productId,name:i.name,imageUrl:i.imageUrl||"",price:i.price,qty:i.qty}));
  try{
    await db.collection("orders").add({ customerUid:ME.uid, storeId:CART[0].storeId, items, total, status:"PENDING", createdAt:FV.serverTimestamp() });
    CART=[]; saveCart(); toast("Order placed! 🎉","ok"); go("orders");
  }catch(e){ toast("Failed: "+e.message,"bad"); }
}

/* ---------- orders ---------- */
async function renderOrders(){
  $("view-orders").innerHTML=`<h2 class="sec">My orders</h2><div class="panel"><div class="muted center">Loading…</div></div>`;
  let list=[];
  try{ list=(await db.collection("orders").where("customerUid","==",ME.uid).get()).docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt)); }
  catch(e){ return toast("Failed: "+e.message,"bad"); }
  const stTag={PENDING:"wait",ACTIVE:"info",COMPLETED:"ok",CANCELLED:"no"};
  $("view-orders").innerHTML=`<h2 class="sec">My orders <span class="crumb">${list.length}</span></h2>
    ${list.length?list.map(o=>`<div class="panel"><div class="between">
      <b>#${(o.id||"").slice(0,6).toUpperCase()}</b><span class="tag ${stTag[o.status]||"info"}">${o.status||"PENDING"}</span></div>
      <div class="crumb" style="margin:6px 0">${fmtTs(o.createdAt)}</div>
      ${(o.items||[]).map(i=>`<div class="between" style="font-size:14px"><span>${esc(i.name)} × ${i.qty}</span><span>${money(i.price*i.qty)}</span></div>`).join("")}
      <div class="between" style="margin-top:8px"><b>Total</b><b>${money(o.total)}</b></div></div>`).join("")
    :`<div class="empty">No orders yet.</div>`}`;
}

/* ---------- bookings / requests / appointments ---------- */
let bookTab="bookings";
async function renderBookings(){
  $("view-bookings").innerHTML=`<h2 class="sec">My activity</h2>
    <div class="chips">
      <div class="chip"></div></div><div class="panel"><div class="muted center">Loading…</div></div>`;
  let bk=[],rq=[],ap=[];
  try{
    [bk,rq,ap]=await Promise.all([
      db.collection("bookings").where("customerUid","==",ME.uid).get().then(s=>s.docs.map(d=>({id:d.id,...d.data()}))),
      db.collection("serviceRequests").where("customerUid","==",ME.uid).get().then(s=>s.docs.map(d=>({id:d.id,...d.data()}))),
      db.collection("appointments").where("customerUid","==",ME.uid).get().then(s=>s.docs.map(d=>({id:d.id,...d.data()})))
    ]);
  }catch(e){ return toast("Failed: "+e.message,"bad"); }
  bk.sort((a,b)=>tsMs(b.scheduledAt)-tsMs(a.scheduledAt)); ap.sort((a,b)=>tsMs(b.scheduledAt)-tsMs(a.scheduledAt)); rq.sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt));
  const tab=(k,l,n)=>`<button class="${bookTab===k?"btn":"btn alt"}" style="padding:8px 14px" onclick="bookTab='${k}';renderBookings()">${l} (${n})</button>`;
  const stTag=s=>({NEW:"wait",CONFIRMED:"info",DONE:"ok",CANCELLED:"no",IN_PROGRESS:"info",REJECTED:"no"}[s]||"info");
  let body="";
  if(bookTab==="bookings") body=bk.length?bk.map(b=>`<div class="panel"><div class="between"><b>${esc(b.service||"Service")}</b><span class="tag ${stTag(b.status)}">${b.status}</span></div><div class="crumb" style="margin-top:5px">📅 ${fmtTs(b.scheduledAt)}</div></div>`).join(""):`<div class="empty">No bookings.</div>`;
  if(bookTab==="requests") body=rq.length?rq.map(r=>`<div class="panel"><div class="between"><b>${esc(r.title||"Request")}</b><span class="tag ${stTag(r.status)}">${r.status}</span></div><div class="crumb" style="margin-top:5px">${esc(r.details||"")}</div><div class="crumb">${fmtTs(r.createdAt)}</div></div>`).join(""):`<div class="empty">No service requests.</div>`;
  if(bookTab==="appointments") body=ap.length?ap.map(a=>`<div class="panel"><div class="between"><b>${esc(a.purpose||"Appointment")}</b><span class="tag ${stTag(a.status)}">${a.status}</span></div><div class="crumb" style="margin-top:5px">🗓️ ${fmtTs(a.scheduledAt)}</div></div>`).join(""):`<div class="empty">No appointments.</div>`;
  $("view-bookings").innerHTML=`<h2 class="sec">My activity</h2>
    <div class="row" style="margin-bottom:14px;flex-wrap:wrap">${tab("bookings","Bookings",bk.length)}${tab("requests","Requests",rq.length)}${tab("appointments","Appointments",ap.length)}</div>${body}`;
}
function dtInput(id){ const d=new Date(Date.now()+3600000); const v=new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16); return `<input id="${id}" type="datetime-local" value="${v}">`; }
function openBooking(preset){
  if(!CUR||CUR.kind!=="service") return;
  modal(`<h3>Book a service</h3><p class="crumb">${esc(CUR.item.name)}</p>
    <label>What do you need?</label><input id="bk-svc" value="${esc(preset||"")}" placeholder="e.g. Tap repair">
    <label>When?</label>${dtInput("bk-when")}
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveBooking()">Confirm booking</button></div>`);
}
async function saveBooking(){
  const svc=$("bk-svc").value.trim(); if(!svc) return toast("Describe the service","bad");
  const when=$("bk-when").value?TS.fromDate(new Date($("bk-when").value)):TS.now();
  try{ await db.collection("bookings").add({customerUid:ME.uid,providerId:CUR.item.id,service:svc,scheduledAt:when,status:"NEW",createdAt:FV.serverTimestamp()});
    toast("Booking sent ✅","ok"); closeModal(); go("bookings"); }catch(e){ toast("Failed: "+e.message,"bad"); }
}
function openRequest(){
  if(!CUR||CUR.kind!=="service") return;
  modal(`<h3>Request a job</h3><p class="crumb">${esc(CUR.item.name)}</p>
    <label>Title</label><input id="rq-title" placeholder="e.g. Fix leaking pipe">
    <label>Details</label><textarea id="rq-det" rows="3" placeholder="Describe the work…"></textarea>
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveRequest()">Send request</button></div>`);
}
async function saveRequest(){
  const title=$("rq-title").value.trim(); if(!title) return toast("Add a title","bad");
  try{ await db.collection("serviceRequests").add({customerUid:ME.uid,providerId:CUR.item.id,title,details:$("rq-det").value.trim(),status:"NEW",createdAt:FV.serverTimestamp()});
    toast("Request sent ✅","ok"); closeModal(); go("bookings"); }catch(e){ toast("Failed: "+e.message,"bad"); }
}
function openAppointment(){
  if(!CUR||CUR.kind!=="store") return;
  modal(`<h3>Book appointment</h3><p class="crumb">${esc(CUR.item.name)}</p>
    <label>Purpose</label><input id="ap-purpose" placeholder="e.g. Meeseva — Aadhaar update">
    <label>When?</label>${dtInput("ap-when")}
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveAppointment()">Confirm</button></div>`);
}
async function saveAppointment(){
  const purpose=$("ap-purpose").value.trim(); if(!purpose) return toast("Add a purpose","bad");
  const when=$("ap-when").value?TS.fromDate(new Date($("ap-when").value)):TS.now();
  try{ await db.collection("appointments").add({customerUid:ME.uid,storeOrProviderId:CUR.item.id,purpose,scheduledAt:when,status:"NEW"});
    toast("Appointment booked ✅","ok"); closeModal(); go("bookings"); }catch(e){ toast("Failed: "+e.message,"bad"); }
}

/* ---------- reviews ---------- */
async function openReviews(){
  if(!CUR) return; const {kind,item}=CUR; const targetType=kind==="store"?"store":"service";
  let reviews=[];
  try{ reviews=(await db.collection("reviews").where("targetType","==",targetType).where("targetId","==",item.id).get()).docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt)); }catch(e){}
  modal(`<h3>Reviews · ${esc(item.name)}</h3>
    <div style="max-height:260px;overflow:auto;margin:8px 0">
      ${reviews.length?reviews.map(r=>`<div class="listrow"><div style="flex:1"><b>${esc(r.customerName||"Customer")}</b> <span class="stars">${"★".repeat(r.rating||0)}</span><div class="crumb">${esc(r.comment||"")}</div></div></div>`).join(""):'<div class="empty">No reviews yet. Be the first!</div>'}
    </div>
    <label>Your rating</label>
    <select id="rv-rating">${[5,4,3,2,1].map(n=>`<option value="${n}">${"★".repeat(n)} (${n})</option>`).join("")}</select>
    <label>Comment</label><textarea id="rv-comment" rows="2" placeholder="Share your experience…"></textarea>
    <div class="actions"><button class="ghost" onclick="closeModal()">Close</button><button class="btn" onclick="saveReview()">Post review</button></div>`);
}
async function saveReview(){
  const {kind,item}=CUR; const targetType=kind==="store"?"store":"service";
  const rating=parseInt($("rv-rating").value,10), comment=$("rv-comment").value.trim();
  try{
    await db.collection("reviews").add({targetType,targetId:item.id,customerUid:ME.uid,customerName:ME.name||"Customer",rating,comment,createdAt:FV.serverTimestamp()});
    await recomputeRating(targetType,item.id);
    toast("Thanks for your review! ⭐","ok"); closeModal(); loadAll();
  }catch(e){ toast("Failed: "+e.message,"bad"); }
}
async function recomputeRating(targetType,targetId){
  try{
    const snap=await db.collection("reviews").where("targetType","==",targetType).where("targetId","==",targetId).get();
    const r=snap.docs.map(d=>d.data()); const count=r.length; const avg=count?r.reduce((s,x)=>s+(x.rating||0),0)/count:0;
    const col=targetType==="service"?"services":"stores";
    await db.collection(col).doc(targetId).update({rating:Math.round(avg*10)/10,ratingCount:count});
  }catch(e){}
}

/* ---------- chat ---------- */
function threadId(sellerUid){ return `${ME.uid}_${sellerUid}`; }
async function startChat(){
  if(!CUR) return; const s=CUR.item; const sellerUid=s.ownerUid;
  if(!sellerUid) return toast("Seller not reachable","bad");
  const id=threadId(sellerUid);
  try{ await db.collection("chatThreads").doc(id).set({customerUid:ME.uid,customerName:ME.name||"Customer",sellerUid,sellerName:s.name,storeId:CUR.kind==="store"?s.id:"",updatedAt:FV.serverTimestamp()},{merge:true}); }catch(e){}
  go("chat"); setTimeout(()=>openThread(id),200);
}
async function renderChat(){
  $("view-chat").innerHTML=`<h2 class="sec">Chats</h2><div class="panel"><div class="muted center">Loading…</div></div>`;
  let threads=[];
  try{ threads=(await db.collection("chatThreads").where("customerUid","==",ME.uid).get()).docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>tsMs(b.updatedAt)-tsMs(a.updatedAt)); }catch(e){ return toast("Failed: "+e.message,"bad"); }
  $("view-chat").innerHTML=`<h2 class="sec">Chats</h2>
    <div class="chatwrap">
      <div class="threads panel" id="threadList">
        ${threads.length?threads.map(t=>`<div class="thread" id="th-${t.id}" onclick="openThread('${t.id}')"><b>${esc(t.sellerName||"Seller")}</b><div class="crumb">${esc(t.lastMessage||"Start chatting…")}</div></div>`).join(""):'<div class="empty">No chats yet. Open a store and tap Chat.</div>'}
      </div>
      <div class="msgs panel" id="msgPanel"><div class="empty" style="margin:auto">Select a conversation</div></div>
    </div>`;
  window._threads=threads;
}
function openThread(id){
  curThreadId=id; const t=(window._threads||[]).find(x=>x.id===id)||{sellerName:"Seller"};
  document.querySelectorAll(".thread").forEach(x=>x.classList.remove("on")); $("th-"+id)?.classList.add("on");
  $("msgPanel").innerHTML=`<div class="between" style="padding-bottom:8px;border-bottom:1px solid var(--line)"><b>${esc(t.sellerName)}</b><button class="ghost" onclick="renderChat()">↩</button></div>
    <div class="msglist" id="msgList"><div class="muted center">Loading…</div></div>
    <div class="composer"><input id="msgInput" placeholder="Type a message…" onkeydown="if(event.key==='Enter')sendMsg()"><button class="btn" onclick="sendMsg()">Send</button></div>`;
  if(chatUnsub) chatUnsub();
  chatUnsub=db.collection("chatThreads").doc(id).collection("messages").orderBy("createdAt").onSnapshot(snap=>{
    const msgs=snap.docs.map(d=>d.data());
    const el=$("msgList"); if(!el) return;
    el.innerHTML=msgs.length?msgs.map(m=>`<div class="bub ${m.senderUid===ME.uid?"me":"them"}">${esc(m.text)}</div>`).join(""):'<div class="empty" style="margin:auto">Say hello 👋</div>';
    el.scrollTop=el.scrollHeight;
  },e=>{ const el=$("msgList"); if(el) el.innerHTML=`<div class="empty">${e.message}</div>`; });
}
async function sendMsg(){
  const inp=$("msgInput"); const text=inp.value.trim(); if(!text||!curThreadId) return; inp.value="";
  try{
    await db.collection("chatThreads").doc(curThreadId).collection("messages").add({senderUid:ME.uid,text,createdAt:FV.serverTimestamp()});
    await db.collection("chatThreads").doc(curThreadId).set({lastMessage:text,updatedAt:FV.serverTimestamp()},{merge:true});
  }catch(e){ toast("Failed: "+e.message,"bad"); }
}

/* ---------- notifications ---------- */
async function loadNotifBadge(){
  try{ const s=await db.collection("notifications").where("toUid","==",ME.uid).get();
    const unread=s.docs.filter(d=>!d.data().read).length; const b=$("notifBadge"); b.style.display=unread?"grid":"none"; b.textContent=unread; }catch(e){}
}
async function renderNotifications(){
  $("view-notifications").innerHTML=`<h2 class="sec">Notifications</h2><div class="panel"><div class="muted center">Loading…</div></div>`;
  let list=[];
  try{ list=(await db.collection("notifications").where("toUid","==",ME.uid).get()).docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt)); }catch(e){ return toast("Failed: "+e.message,"bad"); }
  for(const n of list.filter(n=>!n.read)){ db.collection("notifications").doc(n.id).update({read:true}).catch(()=>{}); }
  loadNotifBadge();
  $("view-notifications").innerHTML=`<h2 class="sec">Notifications <span class="crumb">${list.length}</span></h2>
    ${list.length?list.map(n=>`<div class="panel" style="${n.read?"":"border-left:3px solid var(--primary)"}"><b>${esc(n.title)}</b><div class="crumb" style="margin-top:4px">${esc(n.body||"")}</div><div class="crumb" style="margin-top:4px">${fmtTs(n.createdAt)}</div></div>`).join(""):`<div class="empty">🔔 No notifications.</div>`}`;
}

/* ---------- account ---------- */
function renderAccount(){
  $("view-account").innerHTML=`<h2 class="sec">My account</h2>
    <div class="panel"><div class="row">
      <div class="av" style="width:64px;height:64px;font-size:24px">${(ME.name||ME.email||"U")[0].toUpperCase()}</div>
      <div style="flex:1"><b style="font-size:18px">${esc(ME.name||"Customer")}</b><div class="crumb">${esc(ME.email||"")}</div><div class="crumb">${esc(ME.phone||"No phone")}</div></div>
    </div>
    <div class="row" style="margin-top:12px;flex-wrap:wrap">
      <button class="ghost" onclick="openProfile()">✏️ Edit profile</button>
      <button class="ghost" onclick="openLocation()">📍 ${LOC?.label?esc(LOC.label):"Set location"}</button>
      <button class="ghost" onclick="toggleTheme()">🌓 Theme</button>
      <button class="ghost" onclick="go('orders')">🧾 Orders</button>
      <button class="ghost" onclick="go('bookings')">📅 Bookings</button>
    </div></div>
    <div class="panel center"><button class="btn alt" onclick="logout()" style="color:var(--red)">↩ Log out</button></div>`;
}
function openProfile(){
  modal(`<h3>Edit profile</h3>
    <label>Name</label><input id="pf-name" value="${esc(ME.name)}">
    <label>Phone</label><input id="pf-phone" value="${esc(ME.phone)}">
    <label>Address / area</label><input id="pf-addr" value="${esc(ME.address)}">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveProfile()">Save</button></div>`);
}
async function saveProfile(){
  const data={name:$("pf-name").value.trim(),phone:$("pf-phone").value.trim(),address:$("pf-addr").value.trim()};
  try{ await db.collection("users").doc(ME.uid).update(data); ME={...ME,...data}; $("meAv").textContent=(ME.name||"U")[0].toUpperCase(); toast("Profile saved","ok"); closeModal(); renderAccount(); }
  catch(e){ toast("Failed: "+e.message,"bad"); }
}

/* ---------- location ---------- */
function openLocation(){
  modal(`<h3>Set your location</h3>
    <button class="btn block" onclick="useMyLocation()">📍 Use my current location</button>
    <div class="or">or enter area</div>
    <input id="loc-text" placeholder="e.g. Madhapur, Hyderabad" value="${esc(LOC?.label||"")}">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveLocText()">Save</button></div>`);
}
function saveLoc(loc){ LOC=loc; try{localStorage.setItem("lk-loc",JSON.stringify(loc));}catch(e){} $("locLabel").textContent=loc.label; }
function saveLocText(){ const t=$("loc-text").value.trim(); if(!t) return toast("Enter an area","bad"); saveLoc({lat:LOC?.lat,lng:LOC?.lng,label:t}); closeModal(); toast("Location set","ok"); go("home"); }
function useMyLocation(){
  if(!navigator.geolocation) return toast("Geolocation not supported","bad");
  toast("Locating…","");
  navigator.geolocation.getCurrentPosition(p=>{ saveLoc({lat:p.coords.latitude,lng:p.coords.longitude,label:"Near you"}); closeModal(); toast("Location set 📍","ok"); go("home"); },
    e=>toast("Couldn't get location: "+e.message,"bad"),{timeout:8000});
}

/* ---------- global search ---------- */
let searchTimer=null;
function onSearch(){ clearTimeout(searchTimer); searchTimer=setTimeout(()=>{
  const q=($("globalSearch").value||"").toLowerCase().trim();
  if(!q){ if($("view-search").classList.contains("active")) go("home"); return; }
  const st=DATA.stores.filter(s=>(s.name||"").toLowerCase().includes(q)||(s.category||"").includes(q));
  const sv=DATA.services.filter(s=>(s.name||"").toLowerCase().includes(q)||(s.category||"").includes(q));
  document.querySelectorAll("#tabs a").forEach(x=>x.classList.remove("active"));
  document.querySelectorAll(".view").forEach(v=>v.classList.remove("active")); $("view-search").classList.add("active");
  $("view-search").innerHTML=`<h2 class="sec">Results for “${esc(q)}” <span class="crumb">${st.length+sv.length} found</span></h2>
    ${st.length?`<h2 class="sec">Stores</h2><div class="grid">${st.map(s=>storeCardHtml(s,"store")).join("")}</div>`:""}
    ${sv.length?`<h2 class="sec">Services</h2><div class="grid">${sv.map(s=>storeCardHtml(s,"service")).join("")}</div>`:""}
    ${!st.length&&!sv.length?`<div class="empty">No matches.</div>`:""}`;
},250); }
