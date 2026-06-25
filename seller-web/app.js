/* LocalKart Seller — web app. Same Firebase project as the Android apps. */
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
let ME = null;
const MY = { stores: [], services: [], products: [], offerings: [], orders: [], bookings: [], requests: [], appointments: [] };
let BILLING = {}, PLANS = [], CATS = [];
let SIGNUP = false, chatUnsub = null, curThreadId = null, threadsCache = [];

/* ---------- theme ---------- */
function applyTheme(t){ document.body.classList.toggle("dark", t==="dark"); }
function toggleTheme(){ const n=document.body.classList.contains("dark")?"light":"dark"; try{localStorage.setItem("lks-theme",n)}catch(e){} applyTheme(n); }
applyTheme((()=>{try{return localStorage.getItem("lks-theme")}catch(e){return null}})()||"light");

/* ---------- helpers ---------- */
const $ = id => document.getElementById(id);
function toast(m,k){ const t=document.createElement("div"); t.className="toast "+(k||""); t.textContent=m; $("toast").appendChild(t); setTimeout(()=>t.remove(),3000); }
function modal(html){ $("modalBox").classList.remove("wide"); $("modalBox").innerHTML=html; $("modalBg").classList.add("open"); }
function modalWide(html){ $("modalBox").classList.add("wide"); $("modalBox").innerHTML=html; $("modalBg").classList.add("open"); }
function closeModal(){ $("modalBg").classList.remove("open"); $("modalBox").classList.remove("wide"); }
$("modalBg").addEventListener("click",e=>{ if(e.target.id==="modalBg") closeModal(); });
const num = n => (n||0).toLocaleString("en-IN");
const money = n => "₹"+num(Math.round(n||0));
const esc = s => (s==null?"":String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c])));
const val = id => ($(id)?.value||"").trim();
const catLabel = n => (n||"").replace(/_/g," ").replace(/\b\w/g,c=>c.toUpperCase());
const img = (u,cls) => u ? `<img class="${cls||""}" src="${esc(u)}">` : `<div class="${cls||""}" style="display:grid;place-items:center;font-size:22px;color:var(--muted)">🏬</div>`;
function fmtTs(ts){ if(!ts) return "-"; const ms=ts.seconds?ts.seconds*1000:(ts.toMillis?ts.toMillis():0); return ms?new Date(ms).toLocaleString("en-IN",{dateStyle:"medium",timeStyle:"short"}):"-"; }
function tsMs(ts){ return ts?(ts.seconds?ts.seconds*1000:(ts.toMillis?ts.toMillis():0)):0; }
function opt(v,l,c){ return `<option value="${v}" ${v===c?"selected":""}>${l}</option>`; }
async function qChunks(col,field,ids){ const out=[]; for(let i=0;i<ids.length;i+=10){ const chunk=ids.slice(i,i+10); if(!chunk.length) continue;
  const s=await db.collection(col).where(field,"in",chunk).get(); s.docs.forEach(d=>out.push({id:d.id,...d.data()})); } return out; }
async function uploadImage(fileId,folder){ const f=$(fileId)?.files?.[0]; if(!f) return ""; const ref=storage.ref().child(`${folder}/${Date.now()}_${f.name.replace(/\s+/g,"_")}`); await ref.put(f); return await ref.getDownloadURL(); }
function previewFile(inId,imgId){ const f=$(inId)?.files?.[0]; const im=$(imgId); if(f&&im){ im.src=URL.createObjectURL(f); im.style.display="block"; } }
function catOptions(type,cur){ const cs=CATS.filter(c=>(c.type||"store")===type); if(!cs.length) return `<option value="${cur||""}">${cur?catLabel(cur):"— no categories —"}</option>`;
  let h=""; if(cur&&!cs.some(c=>c.name===cur)) h+=opt(cur,catLabel(cur)+" (current)",cur); return h+cs.map(c=>opt(c.name,catLabel(c.name),cur)).join(""); }

/* ---------- auth ---------- */
function loginErr(m){ $("loginErr").textContent=m||""; }
function toggleMode(){ SIGNUP=!SIGNUP; $("signupName").style.display=SIGNUP?"block":"none"; $("loginBtn").textContent=SIGNUP?"Create account":"Sign in"; $("toggleMode").textContent=SIGNUP?"Have an account? Sign in":"New seller? Create an account"; }
async function loginGoogle(){ loginErr(""); try{ const c=await auth.signInWithPopup(new firebase.auth.GoogleAuthProvider()); await afterLogin(c.user); }catch(e){ loginErr(e.message); } }
async function loginEmail(){
  loginErr(""); const email=val("email"), pass=$("password").value;
  if(!email||!pass) return loginErr("Enter email and password.");
  try{ let c; if(SIGNUP){ c=await auth.createUserWithEmailAndPassword(email,pass); await c.user.updateProfile({displayName:val("name")||email.split("@")[0]}); } else c=await auth.signInWithEmailAndPassword(email,pass); await afterLogin(c.user); }
  catch(e){ loginErr(e.message); }
}
async function ensureUserDoc(u){
  const ref=db.collection("users").doc(u.uid), snap=await ref.get();
  if(!snap.exists){ const d={name:u.displayName||"",email:u.email||"",phone:"",photoUrl:u.photoURL||"",role:"STORE_AND_PROVIDER",address:"",createdAt:FV.serverTimestamp()}; await ref.set(d); return {uid:u.uid,...d}; }
  return {uid:u.uid,...snap.data()};
}
async function afterLogin(u){ ME=await ensureUserDoc(u); startApp(); }
function logout(){ auth.signOut(); location.reload(); }
auth.onAuthStateChanged(async u=>{ if(u){ try{ ME=await ensureUserDoc(u); startApp(); }catch(e){ loginErr(e.message); } } });
function startApp(){ $("login").style.display="none"; $("app").style.display="flex"; $("meAv").textContent=(ME.name||ME.email||"S")[0].toUpperCase(); loadAll(); }

/* ---------- nav ---------- */
$("tabs").addEventListener("click",e=>{ const a=e.target.closest("a"); if(a) go(a.dataset.view); });
function go(view){
  document.querySelectorAll("#tabs a").forEach(x=>x.classList.toggle("active",x.dataset.view===view));
  document.querySelectorAll(".view").forEach(v=>v.classList.remove("active")); $("view-"+view).classList.add("active");
  if(chatUnsub && view!=="chat"){ chatUnsub(); chatUnsub=null; } window.scrollTo(0,0);
  ({dashboard:renderDashboard,shop:renderShop,services:renderServices,products:renderProducts,orders:renderOrders,bookings:renderBookings,chat:renderChat,billing:renderBilling,notifications:renderNotifications,account:renderAccount}[view]||(()=>{}))();
}

/* ---------- data ---------- */
async function loadAll(){
  try{
    const [st,sv,cat,bil,pl]=await Promise.all([
      db.collection("stores").where("ownerUid","==",ME.uid).get(),
      db.collection("services").where("ownerUid","==",ME.uid).get(),
      db.collection("categories").get(),
      db.collection("billing").doc(ME.uid).get(),
      db.collection("plans").get()
    ]);
    MY.stores=st.docs.map(d=>({id:d.id,...d.data()}));
    MY.services=sv.docs.map(d=>({id:d.id,...d.data()}));
    CATS=cat.docs.map(d=>({id:d.id,...d.data()}));
    BILLING=bil.exists?bil.data():{};
    PLANS=pl.docs.map(d=>({id:d.id,...d.data()}));
    const storeIds=MY.stores.map(s=>s.id), svcIds=MY.services.map(s=>s.id);
    MY.products = storeIds.length?await qChunks("products","storeId",storeIds):[];
    MY.offerings = (await db.collection("serviceOfferings").where("ownerUid","==",ME.uid).get()).docs.map(d=>({id:d.id,...d.data()}));
    MY.orders = storeIds.length?await qChunks("orders","storeId",storeIds):[];
    MY.bookings = svcIds.length?await qChunks("bookings","providerId",svcIds):[];
    MY.requests = svcIds.length?await qChunks("serviceRequests","providerId",svcIds):[];
    MY.appointments = (storeIds.concat(svcIds)).length?await qChunks("appointments","storeOrProviderId",storeIds.concat(svcIds)):[];
  }catch(e){ toast("Load failed: "+e.message,"bad"); }
  $("bizName").textContent = MY.stores[0]?.name || MY.services[0]?.name || ME.name || "Your business";
  loadNotifBadge();
  renderDashboard();
}

/* ---------- billing status ---------- */
function billStatus(){
  const b=BILLING, now=Date.now(); const due=tsMs(b.nextDueAt);
  const active=b.subActive && (!due || due>=now);
  if(!b.activationPaid) return {key:"noact",label:"Activation pending",cls:"warn",msg:"Pay the one-time activation fee to publish your listings. Contact admin to activate."};
  if(active){ const days=due?Math.ceil((due-now)/86400000):null; return {key:"active",label:"Subscription active",cls:"ok",msg:`Your subscription is active${days!=null?` · ${days} day(s) left`:""}.`}; }
  if(b.lastPaidAt) return {key:"expired",label:"Subscription expired",cls:"bad",msg:"Your subscription has expired. Renew to keep your listings visible to customers."};
  return {key:"actonly",label:"Activated · subscribe",cls:"warn",msg:"You're activated. Start your monthly subscription to stay visible."};
}
function billBanner(){ const s=billStatus(); return `<div class="banner ${s.cls}"><span>${s.cls==="ok"?"✅":"⚠️"}</span><div><b>${s.label}.</b> ${s.msg}</div></div>`; }

/* ---------- dashboard ---------- */
function renderDashboard(){
  const pendingOrders=MY.orders.filter(o=>o.status==="PENDING").length;
  const dayStart=new Date(); dayStart.setHours(0,0,0,0);
  const todayOrders=MY.orders.filter(o=>tsMs(o.createdAt)>=dayStart.getTime());
  const revenue=MY.orders.filter(o=>o.status==="COMPLETED").reduce((s,o)=>s+(o.total||0),0);
  const newBookings=MY.bookings.filter(b=>b.status==="NEW").length+MY.requests.filter(r=>r.status==="NEW").length;
  const liveProducts=MY.products.filter(p=>p.approved).length, pendingProducts=MY.products.filter(p=>!p.approved).length;
  const kpi=(ic,n,l)=>`<div class="kpi"><div class="ic">${ic}</div><div class="n">${n}</div><div class="l">${l}</div><div class="bar"></div></div>`;
  $("view-dashboard").innerHTML=`
    <h2 class="sec">Welcome back 👋 <span class="crumb">${esc($("bizName").textContent)}</span></h2>
    ${billBanner()}
    <div class="kpis">
      ${kpi("🧾",num(todayOrders.length),"Orders today")}
      ${kpi("⏳",num(pendingOrders),"Pending orders")}
      ${kpi("📅",num(newBookings),"New bookings/requests")}
      ${kpi("💰",money(revenue),"Completed revenue")}
      ${kpi("📦",num(liveProducts),"Live products")}
      ${kpi("🏪",num(MY.stores.length+MY.services.length),"Listings")}
    </div>
    ${pendingProducts?`<div class="banner warn"><span>⏳</span><div>${pendingProducts} item(s) waiting for admin approval before customers can see them.</div></div>`:""}
    <h2 class="sec">Quick actions</h2>
    <div class="row" style="flex-wrap:wrap">
      <button class="btn" onclick="openListing('store')">＋ Add shop</button>
      <button class="btn" onclick="openListing('service')">＋ Add service</button>
      <button class="ghost" onclick="go('products')">📦 Manage products</button>
      <button class="ghost" onclick="go('orders')">🧾 Orders (${MY.orders.length})</button>
      <button class="ghost" onclick="go('bookings')">📅 Bookings (${MY.bookings.length+MY.requests.length+MY.appointments.length})</button>
    </div>
    <h2 class="sec">Recent orders</h2>
    <div class="panel">${MY.orders.slice().sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt)).slice(0,5).map(orderRow).join("")||'<div class="empty">No orders yet.</div>'}</div>`;
}

/* ---------- shop / services listings ---------- */
function listingCard(s,kind){
  const isStore=kind==="store"; const availTag=isStore?(s.isOpen?'<span class="tag ok">Open</span>':'<span class="tag no">Closed</span>'):(s.available?'<span class="tag ok">Available</span>':'<span class="tag no">Busy</span>');
  return `<div class="card"><div class="ph">${img(s.photoUrl)}</div><div class="bd">
    <div class="between"><b>${esc(s.name)}</b>${s.approved?'<span class="tag ok">Live</span>':'<span class="tag wait">Pending approval</span>'}</div>
    <div class="crumb" style="margin:4px 0">${catLabel(s.category)} · ★ ${(s.rating||0).toFixed(1)} (${s.ratingCount||0})</div>
    <div class="crumb">${esc(s.address||"")}</div>
    <div style="margin-top:4px">${isStore?shopTypeBadge(s.shopType)+fulfilBadges(s)+' ':''}${s.showContact?'<span class="tag ok">Contact shared</span>':'<span class="tag wait">Contact hidden</span>'}${s.location?' <span class="tag info">📍 Mapped</span>':''}${s.pincode?` <span class="tag info">PIN ${esc(s.pincode)}</span>`:''}${s.locationApprovalPending?' <span class="tag wait">Location pending</span>':''}</div>
    <div class="row" style="margin-top:10px;flex-wrap:wrap">
      <button class="mini" onclick="openListing('${kind}','${s.id}')">Edit</button>
      <button class="mini" onclick="toggleAvail('${kind}','${s.id}',${isStore?!s.isOpen:!s.available})">${isStore?(s.isOpen?"Mark closed":"Mark open"):(s.available?"Mark busy":"Mark available")}</button>
      ${isStore?`<button class="mini" onclick="go('products')">Products</button>`:`<button class="mini" onclick="openCatalogBrowser('service','${s.id}')">＋ Add services</button>`}
      ${availTag}
    </div></div></div>`;
}
function renderShop(){
  $("view-shop").innerHTML=`<h2 class="sec">My Shop <button class="btn" onclick="openListing('store')">＋ Add shop</button></h2>
    ${billBanner()}
    <div class="grid">${MY.stores.map(s=>listingCard(s,"store")).join("")||'<div class="empty" style="grid-column:1/-1">No shop yet. Click “Add shop” to create your store.</div>'}</div>`;
}
function renderServices(){
  $("view-services").innerHTML=`<h2 class="sec">My Services <button class="btn" onclick="openListing('service')">＋ Add service</button></h2>
    ${billBanner()}
    <div class="grid">${MY.services.map(s=>listingCard(s,"service")).join("")||'<div class="empty" style="grid-column:1/-1">No service profile yet. Click “Add service”.</div>'}</div>
    ${MY.offerings.length?`<h2 class="sec">My service prices</h2><div class="pgrid">${MY.offerings.map(o=>`<div class="card"><div class="ph">${img(o.imageUrl)}</div><div class="bd"><b>${esc(o.name)}</b><div>${priceBlock(o.price,o.mrp)}</div>${o.approved?'<span class="tag ok">Live</span>':'<span class="tag wait">Pending</span>'} <button class="reject" onclick="delDoc('serviceOfferings','${o.id}')">Delete</button></div></div>`).join("")}</div>`:""}`;
}
let editListingId=null,editListingKind=null;
function openListing(kind,id){
  editListingKind=kind; editListingId=id||null;
  const s=id?(kind==="store"?MY.stores:MY.services).find(x=>x.id===id):null;
  const isStore=kind==="store";
  // Pre-fill lat/lng from the pending edit if one is awaiting approval, else the active location.
  const gl=(s&&s.pendingLocation&&typeof s.pendingLocation.latitude==="number")?s.pendingLocation
        :(s&&s.location&&typeof s.location.latitude==="number")?s.location:null;
  const latV=gl?gl.latitude:"" , lngV=gl?gl.longitude:"";
  const pinV=(s&&s.locationApprovalPending&&s.pendingPincode)?s.pendingPincode:(s?.pincode||"");
  modal(`<h3>${id?"Edit":"Add"} ${isStore?"shop":"service"}</h3>
    <label>${isStore?"Shop":"Business"} name</label><input id="l-name" value="${esc(s?.name)}">
    ${isStore?`<label>Shop type</label><select id="l-shoptype">
      ${opt("physical","🏬 Physical store — customers visit your shop",s?.shopType||"physical")}
      ${opt("digital","💻 Online store — no shop, you sell / deliver online",s?.shopType||"physical")}
      ${opt("hybrid","🏬💻 Store + Online — you have a shop and also serve online",s?.shopType||"physical")}
    </select>`:""}
    <label>Category</label><select id="l-cat">${catOptions(isStore?"store":"service",s?.category)}</select>
    ${isStore?`<div style="margin-top:14px;padding-top:10px;border-top:1px solid var(--line)"><b>Fulfilment</b><div class="crumb">How customers get their order.</div></div>
    ${toggleRow("l-delivery","🚚 Door delivery available",!!s?.doorDelivery)}
    ${toggleRow("l-pickup","🛍️ Store pickup available",!!s?.pickup)}`:""}
    <label>Description</label><textarea id="l-desc" rows="2">${esc(s?.description)}</textarea>
    <label>Address / area</label><input id="l-addr" value="${esc(s?.address)}">
    ${isStore?"":`<label>Price per visit (₹)</label><input id="l-ppv" type="number" value="${s?.pricePerVisit??""}">`}
    <label>Shop / cover photo ${id?"(leave empty to keep)":""}</label><input id="l-file" type="file" accept="image/*" onchange="previewFile('l-file','l-prev')">
    <img id="l-prev" src="${s?.photoUrl||""}" style="display:${s?.photoUrl?"block":"none"};width:100%;height:120px;object-fit:cover;border-radius:12px;margin-top:8px">

    <div style="margin-top:16px;padding-top:12px;border-top:1px solid var(--line)"><b>Owner contact</b><div class="crumb">Turn on each item you want customers to see.</div></div>

    <label>Owner photo (optional)</label><input id="l-ownerfile" type="file" accept="image/*" onchange="previewFile('l-ownerfile','l-ownerprev')">
    <img id="l-ownerprev" src="${s?.ownerPhotoUrl||""}" style="display:${s?.ownerPhotoUrl?"block":"none"};width:72px;height:72px;border-radius:50%;object-fit:cover;margin-top:8px">
    ${toggleRow("l-showphoto","Show photo to customers",flagDefault(s,"showPhoto"))}

    <label>Phone number</label><input id="l-phone" value="${esc(s?.phone)}" placeholder="10-digit mobile">
    ${toggleRow("l-showphone","Show phone to customers",flagDefault(s,"showPhone"))}

    <label>WhatsApp number</label><input id="l-wa" value="${esc(s?.whatsapp)}" placeholder="WhatsApp number (with country code if outside India)">
    ${toggleRow("l-showwa","Show WhatsApp to customers",flagDefault(s,"showWhatsapp"))}

    <div style="margin-top:16px;padding-top:12px;border-top:1px solid var(--line)"><b>Location</b><div class="crumb">${s&&s.approved?"Changes to an approved shop's location need admin approval.":"Set your shop's exact spot."}</div></div>
    <label>Pincode</label><input id="l-pincode" value="${esc(pinV)}" placeholder="6-digit PIN code">
    <label>Latitude &amp; Longitude</label>
    <div class="row" style="gap:8px">
      <input id="l-lat" type="number" step="any" value="${latV}" placeholder="Latitude">
      <input id="l-lng" type="number" step="any" value="${lngV}" placeholder="Longitude">
    </div>
    <div class="row" style="margin-top:6px;flex-wrap:wrap"><button class="ghost" type="button" onclick="captureLoc()">📍 Use current location</button>${s&&s.locationApprovalPending?'<span class="tag wait">Location change pending approval</span>':""}</div>
    ${toggleRow("l-showloc","Show map location to customers",flagDefault(s,"showLocation"))}

    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" id="l-save" onclick="saveListing()">Save</button></div>`);
}
// Per-field visibility default: explicit flag if set, else fall back to legacy showContact.
function flagDefault(s,key){ return s && s[key]!==undefined ? !!s[key] : !!(s && s.showContact); }
function shopTypeBadge(t){ t=t||"physical"; if(t==="digital") return '<span class="tag info">💻 Online store</span>'; if(t==="hybrid") return '<span class="tag info">🏬💻 Store + Online</span>'; return '<span class="tag ok">🏬 Physical store</span>'; }
function fulfilBadges(s){ let h=""; if(s.doorDelivery) h+=' <span class="tag info">🚚 Delivery</span>'; if(s.pickup) h+=' <span class="tag info">🛍️ Pickup</span>'; return h; }
function toggleRow(id,label,on){
  return `<label class="switch"><input type="checkbox" id="${id}" ${on?"checked":""}><span class="track"></span><span>${label}</span></label>`;
}
function captureLoc(){
  if(!navigator.geolocation) return toast("Geolocation not supported","bad");
  toast("Locating…");
  navigator.geolocation.getCurrentPosition(p=>{
    if($("l-lat")) $("l-lat").value=p.coords.latitude.toFixed(6);
    if($("l-lng")) $("l-lng").value=p.coords.longitude.toFixed(6);
    toast("Location captured 📍","ok"); },
    e=>toast("Couldn't get location: "+e.message,"bad"),{enableHighAccuracy:true,timeout:9000});
}
async function saveListing(){
  const name=val("l-name"); if(!name) return toast("Name required","bad");
  const isStore=editListingKind==="store"; const btn=$("l-save"); if(btn){btn.disabled=true;btn.textContent="Saving…";}
  try{
    const photoUrl=await uploadImage("l-file",isStore?"stores":"services");
    const ownerPhotoUrl=await uploadImage("l-ownerfile","ownerPhotos");
    const showPhoto=$("l-showphoto")?.checked||false, showPhone=$("l-showphone")?.checked||false,
          showWhatsapp=$("l-showwa")?.checked||false, showLocation=$("l-showloc")?.checked||false;
    const base={name,category:val("l-cat"),description:val("l-desc"),address:val("l-addr"),ownerUid:ME.uid,
      phone:val("l-phone"), whatsapp:val("l-wa"),
      showPhoto, showPhone, showWhatsapp, showLocation,
      showContact:(showPhoto||showPhone||showWhatsapp||showLocation)};
    if(isStore){ base.shopType=val("l-shoptype")||"physical"; base.doorDelivery=$("l-delivery")?.checked||false; base.pickup=$("l-pickup")?.checked||false; }
    if(!isStore) base.pricePerVisit=parseFloat(val("l-ppv")||"0")||0;
    if(photoUrl) base.photoUrl=photoUrl;
    if(ownerPhotoUrl) base.ownerPhotoUrl=ownerPhotoUrl;
    // ---- location + pincode (admin approval for edits to an approved shop) ----
    const lat=parseFloat(val("l-lat")), lng=parseFloat(val("l-lng")); const hasLoc=!isNaN(lat)&&!isNaN(lng);
    const newGeo=hasLoc?new firebase.firestore.GeoPoint(lat,lng):null; const pincode=val("l-pincode");
    const existing=editListingId?(isStore?MY.stores:MY.services).find(x=>x.id===editListingId):null;
    const col=isStore?"stores":"services";
    let locMsg="";
    if(!editListingId || !existing?.approved){
      // new shop, or not-yet-approved: apply directly (part of the normal listing approval)
      if(newGeo) base.location=newGeo;
      base.pincode=pincode; base.locationApprovalPending=false;
    }else{
      const cur=existing.location;
      const moved = newGeo && (!cur || Math.abs((cur.latitude||0)-lat)>1e-6 || Math.abs((cur.longitude||0)-lng)>1e-6);
      const pinChanged = pincode!==(existing.pincode||"");
      if(moved||pinChanged){
        if(newGeo) base.pendingLocation=newGeo;
        base.pendingPincode=pincode; base.locationApprovalPending=true;   // active location stays until admin approves
        locMsg=" Location change sent for admin approval.";
      }
    }
    if(editListingId){ await db.collection(col).doc(editListingId).update(base); }
    else{ const doc={...base,photoUrl:photoUrl||"",rating:0,ratingCount:0,approved:false,createdAt:FV.serverTimestamp()}; if(isStore)doc.isOpen=true; else doc.available=true; await db.collection(col).add(doc); await maybeUpgradeRole(); }
    toast("Saved ✅"+locMsg,"ok"); closeModal(); await loadAll(); go(isStore?"shop":"services");
  }catch(e){ toast("Failed: "+e.message,"bad"); if(btn){btn.disabled=false;btn.textContent="Save";} }
}
async function maybeUpgradeRole(){
  try{ await db.collection("users").doc(ME.uid).update({role:"STORE_AND_PROVIDER"}); ME.role="STORE_AND_PROVIDER"; }catch(e){}
}
async function toggleAvail(kind,id,v){
  const col=kind==="store"?"stores":"services"; const field=kind==="store"?"isOpen":"available";
  try{ await db.collection(col).doc(id).update({[field]:v}); toast("Updated","ok"); await loadAll(); kind==="store"?renderShop():renderServices(); }catch(e){ toast("Failed: "+e.message,"bad"); }
}
async function delDoc(col,id){ if(!confirm("Delete this?")) return; try{ await db.collection(col).doc(id).delete(); toast("Deleted","ok"); await loadAll(); go("services"); }catch(e){ toast("Failed: "+e.message,"bad"); } }

/* ---------- products (store) ---------- */
function priceBlock(price,mrp){ let h=`<span class="price">${money(price)}</span>${mrp>price?`<span class="mrp">${money(mrp)}</span>`:""}`;
  if(mrp>price){ const s=Math.round(mrp-price),p=Math.round((mrp-price)/mrp*100); h+=`<div class="save">Customer saves ${money(s)} (${p}% off)</div>`; } return h; }
function renderProducts(){
  if(!MY.stores.length){ $("view-products").innerHTML=`<h2 class="sec">Products</h2><div class="empty">Create a shop first.<br><button class="btn" style="margin-top:10px" onclick="openListing('store')">＋ Add shop</button></div>`; return; }
  const byStore=MY.stores.map(st=>{
    const ps=MY.products.filter(p=>p.storeId===st.id);
    return `<h2 class="sec">${esc(st.name)} <button class="btn" onclick="openCatalogBrowser('product','${st.id}')">＋ Add products</button></h2>
      <div class="pgrid">${ps.map(p=>`<div class="card"><div class="ph">${img(p.imageUrl)}</div><div class="bd">
        <b>${esc(p.name)}</b>${p.unit?`<div class="crumb">${esc(p.unit)}</div>`:""}<div>${priceBlock(p.price,p.mrp)}</div>
        <div class="between" style="margin-top:6px">${p.approved?'<span class="tag ok">Live</span>':'<span class="tag wait">Pending</span>'} ${p.inStock?'<span class="tag info">In stock</span>':'<span class="tag no">Out</span>'}</div>
        <div class="row" style="margin-top:8px;flex-wrap:wrap">
          <button class="mini" onclick="editPrice('${p.id}')">Edit price</button>
          <button class="mini" onclick="toggleStock('${p.id}',${!p.inStock})">${p.inStock?"Mark out":"In stock"}</button>
          <button class="reject" onclick="delDoc2('products','${p.id}')">Delete</button>
        </div></div></div>`).join("")||'<div class="empty" style="grid-column:1/-1">No products yet — add from the master catalog.</div>'}</div>`;
  }).join("");
  $("view-products").innerHTML=`<h2 class="sec">Products <span class="crumb">pick from company catalog & set your price</span></h2>${billBanner()}${byStore}`;
}
async function delDoc2(col,id){ if(!confirm("Delete this?")) return; try{ await db.collection(col).doc(id).delete(); toast("Deleted","ok"); await loadAll(); renderProducts(); }catch(e){ toast("Failed: "+e.message,"bad"); } }
async function toggleStock(id,v){ try{ await db.collection("products").doc(id).update({inStock:v}); toast("Updated","ok"); await loadAll(); renderProducts(); }catch(e){ toast("Failed: "+e.message,"bad"); } }

let catalogCache={product:null,service:null};
async function loadCatalog(type){ if(catalogCache[type]) return catalogCache[type]; const s=await db.collection("catalog").where("type","==",type).get(); catalogCache[type]=s.docs.map(d=>({id:d.id,...d.data()})); return catalogCache[type]; }

/* ===== Pro catalog browser (products + services) ===== */
let CB={ mode:"product", ownerId:null, items:[], q:"", cat:"all" };
async function openCatalogBrowser(mode, ownerId){
  CB={ mode, ownerId, items:[], q:"", cat:"all" };
  modalWide(`<h3>Add ${mode==="service"?"services":"products"} from catalog</h3>
    <div class="crumb">Pick items, set your price, and add. Goes live after admin approval.</div>
    <input id="cb-q" placeholder="Search ${mode==="service"?"services":"products"}…" oninput="CB.q=this.value.toLowerCase();renderCatBrowser()" style="margin-top:10px">
    <div id="cb-chips" class="row" style="gap:6px;flex-wrap:wrap;margin:10px 0"></div>
    <div id="cb-grid" class="catgrid"><div class="muted center" style="grid-column:1/-1;padding:30px">Loading…</div></div>
    <div class="actions"><button class="ghost" onclick="closeModal()">Done</button></div>`);
  CB.items = await loadCatalog(mode);
  renderCatBrowser();
}
function cbAddedCatalogIds(){
  if(CB.mode==="service") return new Set(MY.offerings.filter(o=>o.providerId===CB.ownerId).map(o=>o.catalogId));
  return new Set(MY.products.filter(p=>p.storeId===CB.ownerId).map(p=>p.catalogId));
}
function renderCatBrowser(){
  const cats=[...new Set(CB.items.map(i=>i.category).filter(Boolean))].sort();
  const chips=$("cb-chips"); if(chips){
    chips.innerHTML=[`<button class="${CB.cat==="all"?"btn":"btn alt"}" style="padding:6px 12px;font-size:12px" onclick="CB.cat='all';renderCatBrowser()">All</button>`]
      .concat(cats.map(c=>`<button class="${CB.cat===c?"btn":"btn alt"}" style="padding:6px 12px;font-size:12px" onclick="CB.cat='${esc(c)}';renderCatBrowser()">${catLabel(c)}</button>`)).join("");
  }
  const added=cbAddedCatalogIds();
  const list=CB.items.filter(i=>(CB.cat==="all"||i.category===CB.cat) && ((i.name||"")+" "+(i.category||"")).toLowerCase().includes(CB.q));
  const grid=$("cb-grid"); if(!grid) return;
  grid.innerHTML=list.length?list.map(i=>{
    const isAdded=added.has(i.id);
    return `<div class="catcard"><div class="cph">${img(i.imageUrl)}</div><div class="cbd">
      <div class="nm">${esc(i.name)}</div>
      <div class="crumb">${catLabel(i.category)}${i.unit?" · "+esc(i.unit):""}</div>
      ${isAdded?`<span class="tag ok">✓ Added</span>`:`
        <div class="pin"><input id="m-${i.id}" type="number" placeholder="MRP" value="${i.suggestedMrp||""}" oninput="cbPrev('${i.id}')">
          <input id="p-${i.id}" type="number" placeholder="Your ₹" oninput="cbPrev('${i.id}')"></div>
        <div class="save" id="s-${i.id}"></div>
        <button class="addbtn" onclick="cbAdd('${i.id}')">＋ Add</button>`}
    </div></div>`;
  }).join(""):'<div class="empty" style="grid-column:1/-1">No items here. Ask admin to load the default catalog.</div>';
}
function cbItem(id){ return CB.items.find(x=>x.id===id); }
function cbPrev(id){ const mrp=parseFloat(val("m-"+id)||"0"),price=parseFloat(val("p-"+id)||"0"); const el=$("s-"+id); if(!el)return;
  if(mrp>price&&price>0){ const s=Math.round(mrp-price),p=Math.round((mrp-price)/mrp*100); el.textContent=`Customer saves ${money(s)} (${p}% off)`; } else el.textContent=""; }
async function cbAdd(id){
  const i=cbItem(id); if(!i) return;
  let mrp=parseFloat(val("m-"+id)||"0")||0; let price=parseFloat(val("p-"+id)||"0")||0;
  if(price<=0){ if(i.suggestedMrp){ price=i.suggestedMrp; mrp=mrp||i.suggestedMrp; } else return toast("Enter your price","bad"); }
  if(!mrp) mrp=price;
  try{
    if(CB.mode==="service"){
      await db.collection("serviceOfferings").add({ ownerUid:ME.uid, providerId:CB.ownerId, catalogId:i.id, name:i.name, category:i.category||"", imageUrl:i.imageUrl||"", price, mrp, approved:false });
    }else{
      await db.collection("products").add({ storeId:CB.ownerId, catalogId:i.id, name:i.name, category:i.category||"", imageUrl:i.imageUrl||"", unit:i.unit||"", price, mrp, inStock:true, approved:false });
    }
    toast(`Added ${i.name} — pending approval`,"ok");
    await loadAll(); renderCatBrowser();   // refresh "Added" markers, keep browser open for more
  }catch(e){ toast("Failed: "+e.message,"bad"); }
}
function editPrice(id){
  const p=MY.products.find(x=>x.id===id); if(!p) return;
  modal(`<h3>Edit price</h3><p class="crumb">${esc(p.name)}</p>
    <label>MRP (₹)</label><input id="ep-mrp" type="number" value="${p.mrp||""}">
    <label>Selling price (₹)</label><input id="ep-price" type="number" value="${p.price||""}">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveEditPrice('${id}')">Save</button></div>`);
}
async function saveEditPrice(id){ const mrp=parseFloat(val("ep-mrp")||"0")||0,price=parseFloat(val("ep-price")||"0")||0;
  try{ await db.collection("products").doc(id).update({mrp,price}); toast("Price updated","ok"); closeModal(); await loadAll(); renderProducts(); }catch(e){ toast("Failed: "+e.message,"bad"); } }

/* ---------- offerings (service prices) ---------- */
async function openOffering(providerId){
  modal(`<h3>Add service price</h3><input id="cp-q" placeholder="Search services…" oninput="filterOfferPick()"><div id="cp-list" style="max-height:340px;overflow:auto;margin-top:10px"><div class="muted center">Loading…</div></div>
    <div class="actions"><button class="ghost" onclick="closeModal()">Close</button></div>`);
  const items=await loadCatalog("service"); window._cpProvider=providerId; window._cpItems=items; renderOfferPick(items);
}
function filterOfferPick(){ const q=val("cp-q").toLowerCase(); renderOfferPick((window._cpItems||[]).filter(i=>(i.name||"").toLowerCase().includes(q)||(i.category||"").includes(q))); }
function renderOfferPick(items){
  $("cp-list").innerHTML=items.length?items.map(i=>`<div class="listrow">${img(i.imageUrl,"thumb")}<div style="flex:1"><b>${esc(i.name)}</b><div class="crumb">${catLabel(i.category)}</div></div>
    <button class="btn" onclick='pickOffer(${JSON.stringify(i).replace(/'/g,"&#39;")})'>Set price</button></div>`).join(""):'<div class="empty">No service catalog items.</div>';
}
function pickOffer(i){
  modal(`<h3>Set your price</h3><p class="crumb">${esc(i.name)}</p>
    <label>MRP (₹)</label><input id="pp-mrp" type="number" value="${i.suggestedMrp||""}" oninput="ppPreview()">
    <label>Your price (₹)</label><input id="pp-price" type="number" oninput="ppPreview()">
    <div id="pp-prev" class="save" style="margin-top:8px"></div>
    <div class="banner warn" style="margin-top:10px"><span>⏳</span><div>Goes live after admin approval.</div></div>
    <div class="actions"><button class="ghost" onclick="openOffering(window._cpProvider)">← Back</button><button class="btn" id="pp-save" onclick='saveOffer(${JSON.stringify(i).replace(/'/g,"&#39;")})'>Add service</button></div>`);
}
async function saveOffer(i){
  const mrp=parseFloat(val("pp-mrp")||"0")||0, price=parseFloat(val("pp-price")||"0")||0; if(price<=0) return toast("Enter a price","bad");
  try{ await db.collection("serviceOfferings").add({ ownerUid:ME.uid, providerId:window._cpProvider, catalogId:i.id, name:i.name, category:i.category||"", imageUrl:i.imageUrl||"", price, mrp, approved:false });
    toast("Added — pending approval","ok"); closeModal(); await loadAll(); renderServices(); }catch(e){ toast("Failed: "+e.message,"bad"); }
}

/* ---------- orders ---------- */
const ORDER_ST=["PENDING","ACTIVE","COMPLETED","CANCELLED"], BOOK_ST=["NEW","CONFIRMED","DONE","CANCELLED"], REQ_ST=["NEW","IN_PROGRESS","DONE","REJECTED"];
function statusSel(col,id,cur,opts){ return `<select class="statussel" onchange="setStatus('${col}','${id}',this.value)">${opts.map(o=>opt(o,o,cur)).join("")}</select>`; }
async function setStatus(col,id,v){ try{ await db.collection(col).doc(id).update({status:v}); toast("Status updated","ok"); await loadAll(); }catch(e){ toast("Failed: "+e.message,"bad"); } }
function orderRow(o){ const stTag={PENDING:"wait",ACTIVE:"info",COMPLETED:"ok",CANCELLED:"no"};
  return `<div class="listrow"><div style="flex:1"><b>#${(o.id||"").slice(0,6).toUpperCase()}</b> <span class="tag ${stTag[o.status]||"info"}">${o.status}</span>
    <div class="crumb">${(o.items||[]).map(i=>`${esc(i.name)}×${i.qty}`).join(", ")}</div><div class="crumb">${fmtTs(o.createdAt)}</div></div>
    <b style="width:90px;text-align:right">${money(o.total)}</b>${statusSel("orders",o.id,o.status,ORDER_ST)}</div>`; }
function renderOrders(){
  const list=MY.orders.slice().sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt));
  $("view-orders").innerHTML=`<h2 class="sec">Incoming orders <span class="crumb">${list.length}</span></h2>
    <div class="panel">${list.map(orderRow).join("")||'<div class="empty">No orders yet.</div>'}</div>`;
}

/* ---------- bookings / requests / appointments ---------- */
let bookTab="bookings";
function renderBookings(){
  const stTag=s=>({NEW:"wait",CONFIRMED:"info",DONE:"ok",CANCELLED:"no",IN_PROGRESS:"info",REJECTED:"no"}[s]||"info");
  const tab=(k,l,n)=>`<button class="${bookTab===k?"btn":"btn alt"}" style="padding:8px 14px" onclick="bookTab='${k}';renderBookings()">${l} (${n})</button>`;
  let body="";
  if(bookTab==="bookings"){ const l=MY.bookings.slice().sort((a,b)=>tsMs(b.scheduledAt)-tsMs(a.scheduledAt));
    body=l.length?l.map(b=>`<div class="listrow"><div style="flex:1"><b>${esc(b.service||"Service")}</b> <span class="tag ${stTag(b.status)}">${b.status}</span><div class="crumb">📅 ${fmtTs(b.scheduledAt)}</div></div>${statusSel("bookings",b.id,b.status,BOOK_ST)}</div>`).join(""):'<div class="empty">No bookings.</div>'; }
  if(bookTab==="requests"){ const l=MY.requests.slice().sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt));
    body=l.length?l.map(r=>`<div class="listrow"><div style="flex:1"><b>${esc(r.title||"Request")}</b> <span class="tag ${stTag(r.status)}">${r.status}</span><div class="crumb">${esc(r.details||"")}</div><div class="crumb">${fmtTs(r.createdAt)}</div></div>${statusSel("serviceRequests",r.id,r.status,REQ_ST)}</div>`).join(""):'<div class="empty">No requests.</div>'; }
  if(bookTab==="appointments"){ const l=MY.appointments.slice().sort((a,b)=>tsMs(b.scheduledAt)-tsMs(a.scheduledAt));
    body=l.length?l.map(a=>`<div class="listrow"><div style="flex:1"><b>${esc(a.purpose||"Appointment")}</b> <span class="tag ${stTag(a.status)}">${a.status}</span><div class="crumb">🗓️ ${fmtTs(a.scheduledAt)}</div></div>${statusSel("appointments",a.id,a.status,BOOK_ST)}</div>`).join(""):'<div class="empty">No appointments.</div>'; }
  $("view-bookings").innerHTML=`<h2 class="sec">Bookings & requests</h2>
    <div class="row" style="margin-bottom:14px;flex-wrap:wrap">${tab("bookings","Bookings",MY.bookings.length)}${tab("requests","Requests",MY.requests.length)}${tab("appointments","Appointments",MY.appointments.length)}</div>
    <div class="panel">${body}</div>`;
}

/* ---------- chat ---------- */
async function renderChat(){
  $("view-chat").innerHTML=`<h2 class="sec">Customer chats</h2><div class="panel"><div class="muted center">Loading…</div></div>`;
  let threads=[];
  try{ threads=(await db.collection("chatThreads").where("sellerUid","==",ME.uid).get()).docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>tsMs(b.updatedAt)-tsMs(a.updatedAt)); }catch(e){ return toast("Failed: "+e.message,"bad"); }
  threadsCache=threads;
  $("view-chat").innerHTML=`<h2 class="sec">Customer chats</h2><div class="chatwrap">
    <div class="threads panel">${threads.length?threads.map(t=>`<div class="thread" id="th-${t.id}" onclick="openThread('${t.id}')"><b>${esc(t.customerName||"Customer")}</b><div class="crumb">${esc(t.lastMessage||"…")}</div></div>`).join(""):'<div class="empty">No chats yet.</div>'}</div>
    <div class="msgs panel" id="msgPanel"><div class="empty" style="margin:auto">Select a conversation</div></div></div>`;
}
function openThread(id){
  curThreadId=id; const t=threadsCache.find(x=>x.id===id)||{customerName:"Customer"};
  document.querySelectorAll(".thread").forEach(x=>x.classList.remove("on")); $("th-"+id)?.classList.add("on");
  $("msgPanel").innerHTML=`<div class="between" style="padding-bottom:8px;border-bottom:1px solid var(--line)"><b>${esc(t.customerName)}</b></div>
    <div class="msglist" id="msgList"><div class="muted center">Loading…</div></div>
    <div class="composer"><input id="msgInput" placeholder="Reply…" onkeydown="if(event.key==='Enter')sendMsg()"><button class="btn" onclick="sendMsg()">Send</button></div>`;
  if(chatUnsub) chatUnsub();
  chatUnsub=db.collection("chatThreads").doc(id).collection("messages").orderBy("createdAt").onSnapshot(snap=>{
    const el=$("msgList"); if(!el) return; const msgs=snap.docs.map(d=>d.data());
    el.innerHTML=msgs.length?msgs.map(m=>`<div class="bub ${m.senderUid===ME.uid?"me":"them"}">${esc(m.text)}</div>`).join(""):'<div class="empty" style="margin:auto">No messages yet</div>';
    el.scrollTop=el.scrollHeight;
  });
}
async function sendMsg(){ const inp=$("msgInput"),text=inp.value.trim(); if(!text||!curThreadId) return; inp.value="";
  try{ await db.collection("chatThreads").doc(curThreadId).collection("messages").add({senderUid:ME.uid,text,createdAt:FV.serverTimestamp()});
    await db.collection("chatThreads").doc(curThreadId).set({lastMessage:text,updatedAt:FV.serverTimestamp()},{merge:true}); }catch(e){ toast("Failed: "+e.message,"bad"); } }

/* ---------- billing ---------- */
function planForListing(){ const s=MY.stores[0]||MY.services[0]; if(!s) return null; const type=MY.stores[0]?"store":"service";
  return PLANS.find(p=>p.category===s.category&&(p.type===type||p.type==="both"))||PLANS.find(p=>p.category===s.category)||null; }
async function renderBilling(){
  const st=billStatus(); const b=BILLING; const plan=planForListing();
  let pays=[]; try{ pays=(await db.collection("payments").where("uid","==",ME.uid).get()).docs.map(d=>d.data()).sort((a,b)=>tsMs(b.at)-tsMs(a.at)); }catch(e){}
  const due=tsMs(b.nextDueAt);
  $("view-billing").innerHTML=`<h2 class="sec">Billing & subscription</h2>
    ${billBanner()}
    <div class="kpis">
      <div class="kpi"><div class="ic">🔓</div><div class="n">${b.activationPaid?money(b.activationAmount):"—"}</div><div class="l">Activation ${b.activationPaid?"paid":"pending"}</div><div class="bar"></div></div>
      <div class="kpi"><div class="ic">🔁</div><div class="n">${b.monthlyFee?money(b.monthlyFee):(plan?money(plan.monthlyFee):"—")}</div><div class="l">Monthly fee</div><div class="bar"></div></div>
      <div class="kpi"><div class="ic">📆</div><div class="n">${due?fmtTs(b.nextDueAt).split(",")[0]:"—"}</div><div class="l">Next due</div><div class="bar"></div></div>
      <div class="kpi"><div class="ic">${st.cls==="ok"?"✅":"⚠️"}</div><div class="n" style="font-size:17px">${st.label}</div><div class="l">Status</div><div class="bar"></div></div>
    </div>
    ${plan?`<div class="panel"><b>Your plan · ${catLabel(plan.category)}</b>
      <div class="crumb" style="margin-top:6px">One-time activation: <b>${money(plan.activationFee)}</b> · Monthly subscription: <b>${money(plan.monthlyFee)}</b></div>
      <div class="row" style="margin-top:12px;flex-wrap:wrap">
        ${!b.activationPaid?`<button class="btn" onclick="pay('activation')">🔓 Pay activation · ${money(plan.activationFee)}</button>`:""}
        ${b.activationPaid&&st.key!=="active"?`<button class="btn" onclick="pay('monthly')">🔁 ${st.key==="expired"?"Renew":"Start"} subscription · ${money(plan.monthlyFee)}</button>`:""}
        ${st.key==="active"?`<button class="ghost" onclick="pay('monthly')">🔁 Renew now (+1 month) · ${money(plan.monthlyFee)}</button>`:""}
      </div>
      <div class="crumb" style="margin-top:10px">🔒 Secure payment via Razorpay (UPI, cards, netbanking). Your subscription updates instantly after payment.</div></div>`
      :`<div class="panel crumb">No plan set for your category yet. The admin will configure pricing.</div>`}
    <h2 class="sec">Payment history</h2>
    <div class="panel">${pays.length?pays.map(p=>`<div class="listrow"><div style="flex:1"><b>${p.type==="activation"?"Activation":"Monthly subscription"}</b><div class="crumb">${fmtTs(p.at)}</div></div><b>${money(p.amount)}</b></div>`).join(""):'<div class="empty">No payments recorded yet.</div>'}</div>`;
}

/* ---------- razorpay payments ---------- */
async function pay(kind){
  if(typeof Razorpay==="undefined") return toast("Payment library not loaded — refresh and retry","bad");
  let order;
  try{
    toast("Starting secure payment…");
    const create=firebase.functions().httpsCallable("createRazorpayOrder");
    order=(await create({kind})).data;
  }catch(e){ return toast(e.message||"Could not start payment","bad"); }
  const rzp=new Razorpay({
    key:order.keyId, amount:order.amount, currency:order.currency, order_id:order.orderId,
    name:"LocalKart", description:kind==="activation"?"One-time activation fee":"Monthly subscription",
    image:"data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🛍️</text></svg>",
    prefill:{ name:ME.name||"", email:ME.email||"", contact:ME.phone||"" },
    theme:{ color:"#7C3AED" },
    handler:async function(resp){
      try{
        toast("Verifying payment…");
        const verify=firebase.functions().httpsCallable("verifyRazorpayPayment");
        await verify({ razorpay_order_id:resp.razorpay_order_id, razorpay_payment_id:resp.razorpay_payment_id, razorpay_signature:resp.razorpay_signature, kind });
        toast(kind==="activation"?"Activated! 🎉":"Subscription updated! 🎉","ok");
        await loadAll(); go("billing");
      }catch(e){ toast("Verification failed: "+(e.message||e)+". If money was deducted it will reflect shortly.","bad"); }
    }
  });
  rzp.on("payment.failed",()=>toast("Payment failed or cancelled","bad"));
  rzp.open();
}

/* ---------- notifications ---------- */
async function loadNotifBadge(){ try{ const s=await db.collection("notifications").where("toUid","==",ME.uid).get(); const u=s.docs.filter(d=>!d.data().read).length; const bd=$("notifBadge"); bd.style.display=u?"grid":"none"; bd.textContent=u; }catch(e){} }
async function renderNotifications(){
  $("view-notifications").innerHTML=`<h2 class="sec">Notifications</h2><div class="panel"><div class="muted center">Loading…</div></div>`;
  let list=[]; try{ list=(await db.collection("notifications").where("toUid","==",ME.uid).get()).docs.map(d=>({id:d.id,...d.data()})).sort((a,b)=>tsMs(b.createdAt)-tsMs(a.createdAt)); }catch(e){ return toast("Failed: "+e.message,"bad"); }
  for(const n of list.filter(n=>!n.read)) db.collection("notifications").doc(n.id).update({read:true}).catch(()=>{});
  loadNotifBadge();
  $("view-notifications").innerHTML=`<h2 class="sec">Notifications <span class="crumb">${list.length}</span></h2>
    ${list.length?list.map(n=>`<div class="panel" style="${n.read?"":"border-left:3px solid var(--primary)"}"><b>${esc(n.title)}</b><div class="crumb" style="margin-top:4px">${esc(n.body||"")}</div><div class="crumb" style="margin-top:4px">${fmtTs(n.createdAt)}</div></div>`).join(""):'<div class="empty">🔔 No notifications.</div>'}`;
}

/* ---------- account ---------- */
function renderAccount(){
  $("view-account").innerHTML=`<h2 class="sec">My account</h2>
    <div class="panel"><div class="row">
      <div class="av" style="width:64px;height:64px;font-size:24px">${(ME.name||ME.email||"S")[0].toUpperCase()}</div>
      <div style="flex:1"><b style="font-size:18px">${esc(ME.name||"Seller")}</b><div class="crumb">${esc(ME.email||"")}</div><div class="crumb">${esc(ME.phone||"No phone")}</div><div class="crumb">${esc(ME.address||"")}</div></div>
    </div>
    <div class="row" style="margin-top:12px;flex-wrap:wrap">
      <button class="ghost" onclick="openProfile()">✏️ Edit profile</button>
      <button class="ghost" onclick="toggleTheme()">🌓 Theme</button>
      <button class="ghost" onclick="go('billing')">💰 Billing</button>
    </div></div>
    <div class="panel center"><button class="btn alt" onclick="logout()" style="color:var(--red)">↩ Log out</button></div>`;
}
function openProfile(){
  modal(`<h3>Edit profile</h3>
    <label>Name</label><input id="pf-name" value="${esc(ME.name)}">
    <label>Phone</label><input id="pf-phone" value="${esc(ME.phone)}">
    <label>Address</label><input id="pf-addr" value="${esc(ME.address)}">
    <div class="actions"><button class="ghost" onclick="closeModal()">Cancel</button><button class="btn" onclick="saveProfile()">Save</button></div>`);
}
async function saveProfile(){ const data={name:val("pf-name"),phone:val("pf-phone"),address:val("pf-addr")};
  try{ await db.collection("users").doc(ME.uid).update(data); ME={...ME,...data}; $("meAv").textContent=(ME.name||"S")[0].toUpperCase(); toast("Saved","ok"); closeModal(); renderAccount(); }catch(e){ toast("Failed: "+e.message,"bad"); } }
