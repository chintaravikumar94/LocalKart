# LocalKart Admin Console — setup & use

The admin web app (`admin-web/`) lets you log in, see live counts, and **approve/reject** sellers (which flips `approved` on their store/service so they appear to customers).

## 1. Add your Firebase web config
1. Firebase Console → ⚙ **Project settings → General → Your apps → Add app → Web (`</>`)**.
2. Register it (name "LocalKart Admin", no Hosting needed) and copy the `firebaseConfig` object.
3. Open `admin-web/app.js` and replace the placeholder `firebaseConfig` at the top with yours.
   - The moment `apiKey` is no longer `"YOUR_API_KEY"`, the app switches from demo mode to **live**.

## 2. Allow Google sign-in for the web app
- Firebase Console → **Authentication → Settings → Authorized domains** → make sure `localhost` is listed (it is by default). Add your hosting domain later if you deploy.

## 3. Make yourself an admin
The console only lets in users listed in the `admins` collection.
1. Sign into either mobile app once (so your user exists), or sign into the admin with Google once — it will say "Not an admin account" and sign you out; that's expected until step 2.
2. Firebase Console → **Firestore → Start collection → `admins`**.
3. **Document ID** = your **UID** (Authentication → Users → copy your UID).
4. Add any field, e.g. `role: "admin"`. Save.

## 4. Run it
```powershell
cd C:\Projects\LocalKart\admin-web
npx serve .
```
Open the printed URL (e.g. http://localhost:3000) → **Sign in with Google** (your admin account) → you're in.

## Using it
- **Dashboard** — live counts (customers, stores, services, pending approvals) + recent sellers.
- **Approvals** — click **Approve** to publish a seller (sets `approved = true`, so their store/service shows to customers and in radius/category queries). **Reject** sets it back to unapproved.
- **Stores / Services / Customers / Grow Items / Banners** — live lists.

> Approvals require the Firestore rules you published (admin update is allowed via `isAdmin()`), plus your `admins/{uid}` doc. If Approve fails with a permission error, re-check both.

## Deploy (optional)
```powershell
firebase init hosting   # public dir: admin-web
firebase deploy --only hosting
```
Then add your `*.web.app` domain to Authentication → Authorized domains.
