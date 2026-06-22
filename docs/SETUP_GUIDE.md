# LocalKart — Step-by-Step Setup Guide

Follow these in order. Each step says exactly what to click/type. Don't skip — later steps depend on earlier ones.

---

## STEP 1 — Install the tools (one time)

1. **Android Studio** — download from https://developer.android.com/studio and install (default options). This also installs the Android SDK and JDK.
2. During first launch, let it download the **Android SDK Platform 34** (Settings → SDK Manager → tick "Android 14.0 (API 34)").
3. **Node.js** (for the admin web) — download "LTS" from https://nodejs.org and install.

You now have everything needed. No need to install Kotlin or Gradle separately — Android Studio handles them.

---

## STEP 2 — Create your Firebase project (one time)

1. Go to https://console.firebase.google.com → **Add project**.
2. Name it `LocalKart` → continue. Google Analytics is optional (you can disable).
3. Wait for it to create, then click **Continue**.

---

## STEP 3 — Turn on the services LocalKart uses

Inside your Firebase project (left sidebar):

1. **Build → Authentication → Get started → Sign-in method tab → Google → Enable → Save.**
   - Set a project support email when asked.
2. **Build → Firestore Database → Create database → Start in production mode → pick your region (e.g. `asia-south1` for India) → Enable.**
3. **Build → Storage → Get started → (same region) → Done.**
4. (Optional, for push) **Build → Cloud Messaging** — nothing to configure yet.

---

## STEP 4 — Register the TWO Android apps in Firebase

You must add both apps so each gets its own config file.

### Customer app
1. Firebase Console → **Project Overview (gear) → Project settings → General → "Your apps" → Add app → Android.**
2. **Android package name:** `com.localkart.customer`
3. App nickname: `LocalKart Customer` (optional).
4. Click **Register app**.
5. **Download `google-services.json`.**
6. In your project, put that file here, replacing the placeholder:
   `C:\Projects\LocalKart\customer-app\google-services.json`
   (delete `google-services.json.PLACEHOLDER`).

### Seller app
Repeat the same steps with:
- **Package name:** `com.localkart.seller`
- Put its `google-services.json` in `C:\Projects\LocalKart\seller-app\google-services.json`.

> ⚠️ The two files are different. Don't swap them.

---

## STEP 5 — Add your signing key (required for Google Sign-In)

Google Sign-In refuses to work without your app's SHA fingerprint.

1. In Android Studio, open the project (see Step 6 first if not opened yet).
2. Open the **Gradle** panel (right edge) → `customer-app → Tasks → android → signingReport` → double-click. (Or run in the terminal: `./gradlew signingReport`.)
3. Copy the **SHA-1** and **SHA-256** values shown for the `debug` variant.
4. Firebase Console → Project settings → Your apps → **com.localkart.customer → Add fingerprint** → paste SHA-1 → save. Repeat for SHA-256.
5. Do the same for **com.localkart.seller**.
6. **Re-download** each `google-services.json` (it now contains an OAuth client) and replace the files from Step 4 again.

---

## STEP 6 — Open and run the apps

1. Android Studio → **Open** → select the folder `C:\Projects\LocalKart`.
2. Wait for **Gradle sync** to finish (bottom status bar). First time downloads dependencies — be patient.
   - If it asks to create a Gradle wrapper, allow it. If sync complains about the wrapper, open the terminal in Android Studio and run: `gradle wrapper` then re-sync.
3. Top toolbar: pick the run target **`customer-app`** and a device (create an emulator via **Device Manager → Create device → Pixel 7 → API 34** if you have none).
4. Press the green **▶ Run**. The Customer app launches; tap **Continue with Google** to sign in.
5. Switch the run target to **`seller-app`** and run it the same way.

---

## STEP 7 — Make yourself an admin + run the web console

1. Sign in once in either app so your user appears in Firestore.
2. Firebase Console → **Firestore → Start collection → Collection ID:** `admins`.
   - **Document ID:** your user's UID (find it in Authentication → Users, copy the UID).
   - Add any field (e.g. `role: "admin"`). Save.
3. Open `C:\Projects\LocalKart\admin-web\app.js` and paste your web config:
   - Firebase Console → Project settings → General → "Your apps" → **Add app → Web (</>)** → register → copy the `firebaseConfig` object → paste it over the placeholder at the top of `app.js`.
4. Run the console locally:
   ```
   cd C:\Projects\LocalKart\admin-web
   npx serve .
   ```
   Open the printed URL (e.g. http://localhost:3000) → sign in with your admin Google account.
   - (Before pasting config it runs in **demo mode** so you can preview the UI immediately.)

---

## STEP 8 — Apply the security rules

1. Firebase Console → **Firestore → Rules** tab.
2. Open `C:\Projects\LocalKart\docs\firestore.rules`, copy everything, paste it in, click **Publish**.
   - This makes listings public-read, lets owners edit only their own, and restricts approvals/banners to admins.

---

## STEP 9 — Add some test data (so screens show real content)

In Firestore, create a `stores` document with fields:
`name` (string), `category` ("groceries"), `approved` (boolean = true), `rating` (number = 4.5), `isOpen` (true).
Add a `services` doc the same way with `category` = "electrician".
Now wire a screen to read them (see "What to build next").

---

## What to build next (recommended order)

1. **Wire Stores Home to live data** — replace the demo list in `StoresMiniApp.kt` with a `FirestoreRepo().storesByCategory(cat)` call inside a ViewModel. This is your reference pattern for every other list.
2. **Real Google Sign-In screen** — connect `AuthScreen` to `AuthManager.googleSignInIntent(...)` using an `ActivityResultLauncher`, then `AuthManager.handleResult(...)`.
3. **Location + radius** — get device location, store a geohash on each store/provider, switch radius filtering to geohash range queries (see README scaling notes).
4. **Orders / Requests / Bookings flows** — write the create + status-update calls.
5. **Push notifications** — FCM tokens saved per user, Cloud Function triggers on new order/request.
6. **Release build** — generate a signed AAB (Build → Generate Signed Bundle) and add the release SHA keys to Firebase before publishing to Play Store.

---

## Common problems & fixes

| Problem | Fix |
|---|---|
| Sign-in fails / "DEVELOPER_ERROR" | SHA fingerprint missing or `google-services.json` not re-downloaded after adding it (Step 5). |
| Gradle sync fails | Check internet; File → Invalidate Caches → Restart. Ensure JDK 17 (Settings → Build Tools → Gradle → Gradle JDK = 17). |
| "Default FirebaseApp is not initialized" | The `google-services.json` is missing or in the wrong module folder. |
| Firestore "Missing index" error | Click the link in the Logcat error — it auto-creates the composite index. |
| Lists are empty | You haven't added data (Step 9) or rules block reads (Step 8). |
| Admin console shows demo numbers | You haven't pasted the web config in `app.js`. |
```
