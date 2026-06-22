# LocalKart — Local Stores & Services Platform

Connecting local stores and service providers to nearby customers. Two Android apps + one admin web console, all on **Cloud Firestore**.

| Piece | Module / Folder | Package | Users target |
|---|---|---|---|
| Customer app | `customer-app/` | `com.localkart.customer` | 50,00,000 |
| Seller app | `seller-app/` | `com.localkart.seller` | 25,00,000 |
| Shared library | `common/` | `com.localkart.common` | — |
| Admin web | `admin-web/` | static HTML/JS | internal |

**Stack:** Native Android (Kotlin + Jetpack Compose + Material 3), Firebase Auth (Google Sign-In), Cloud Firestore, Firebase Storage, FCM. Admin is plain HTML/JS + Firebase Web SDK.

> This is a **code skeleton** — navigation, screens, components, data models, auth wiring and the admin console are in place with demo data. Screens are functional Compose UI ready to be connected to live Firestore queries via `FirestoreRepo`.

---

## App structure

### Customer app (`com.localkart.customer`)
Common header on every page: **More · Local Stores · Local Services · Notifications**.

- **Local Stores** mini-app — bottom nav: `Home · Category · Nearby · Account · Cart`
  - Home: location bar + radius (5/10/15/20/25 km), search, category chips (All, groceries, mobile repairing, fancy, net center, meeseva, household), auto-sliding banners with progress bar, store list.
  - Category: Flipkart-style — left store rail, right services + products grid.
  - Nearby: location bar, round category icons, search, store list.
  - Account: profile card, Orders/Wishlist, recently viewed/purchased.
  - Cart: Flipkart-style cart with price summary.
- **Local Services** mini-app — bottom nav: `Home · Category · Nearby · My Activity`
  - Home: location bar + radius, search, category chips (plumber, electrician, carpenter, gardener, mechanic, housekeeping, cook), banners, nearest-provider list.
  - Category: Flipkart-style left rail + services.
  - My Activity: tabs for Service Requests · Bookings · Appointments.

### Seller app (`com.localkart.seller`)
Common header: **More · Store Owner · Service Provider · Notifications**. Tabs shown depend on signup role — only Store Owner, only Service Provider, or both.

- **Store Owner** — bottom nav: `Home · Grow · Orders · Requests · Appointments · My Shop`
  - Home: welcome, shop card with availability toggle, Orders Today / Sales, banners, PhonePe-style info strip, quick actions.
  - Orders / Requests / Appointments: metric chips + search + status filter chips + list (shared `StatListPage`).
  - My Shop: editable profile, approval status, rating, **dynamic scannable QR ID card**, support/contact/logout.
- **Service Provider** — bottom nav: `Home · Grow · Job Requests · Bookings · My Profile`
  - Same shape, provider-specific metrics and the same QR ID card.

### Admin web (`admin-web/`)
Login (email/password + Google) → dashboard (counts), approvals queue, stores, services, customers, grow items, banners. Runs in **demo mode** until you paste your Firebase config in `app.js`.

---

## Setup

### 1. Firebase
1. Create a Firebase project; enable **Authentication → Google**, **Firestore**, **Storage**, **Cloud Messaging**.
2. Add two Android apps: `com.localkart.customer` and `com.localkart.seller`.
3. Download each `google-services.json` into `customer-app/` and `seller-app/` (replace the `.PLACEHOLDER` files).
4. Add your SHA-1/SHA-256 keys (needed for Google Sign-In) in Firebase project settings.
5. Put the Web SDK config into `admin-web/app.js` and create an `admins/{uid}` doc for each admin.

### 2. Android
- Open the root folder in Android Studio (Giraffe+). It uses the Gradle Kotlin DSL and a version-less wrapper — run `gradle wrapper` once or let Studio generate it.
- Build & run `customer-app` or `seller-app`.
- Min SDK 24, target/compile SDK 34, JDK 17.

### 3. Admin web
```
cd admin-web
npx serve .        # or deploy to Firebase Hosting
```

See `docs/FIRESTORE_SCHEMA.md` for collections and `docs/firestore.rules` for starter security rules.

## Scaling notes (50L+ users)
- Don't filter by distance on the client. Store a **geohash** on each store/provider and use range queries (GeoFirestore) keyed off the selected radius.
- Paginate every list with `limit()` + `startAfter()`.
- Denormalize counters (orders today, ratings) with Cloud Functions / `FieldValue.increment`.
- Composite indexes are required for the `whereEqualTo(...).orderBy(...)` queries in `FirestoreRepo` — Firestore will print the index-creation link on first run.
