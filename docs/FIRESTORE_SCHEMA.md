# Firestore schema — LocalKart

All models live in `common/.../model/Models.kt` (Kotlin data classes with no-arg defaults).

| Collection | Doc id | Key fields | Notes |
|---|---|---|---|
| `users` | uid | name, email, phone, photoUrl, role, location(GeoPoint), address | `role`: CUSTOMER / STORE_OWNER / SERVICE_PROVIDER / STORE_AND_PROVIDER / ADMIN |
| `admins` | uid | (presence only) | Gate for admin console |
| `stores` | auto | ownerUid, name, category, photoUrl, rating, location, isOpen, **approved**, geohash* | category: groceries / mobile_repairing / fancy / net_center / meeseva / household |
| `services` | auto | ownerUid, name, category, available, rating, pricePerVisit, **approved**, geohash* | category: plumber / electrician / carpenter / gardener / mechanic / housekeeping / cook |
| `products` | auto | storeId, name, category, price, mrp, unit, inStock | belongs to a store |
| `orders` | auto | customerUid, storeId, items[], total, status, createdAt | status: PENDING/ACTIVE/COMPLETED/CANCELLED |
| `serviceRequests` | auto | customerUid, providerId, title, details, status, createdAt | status: NEW/IN_PROGRESS/DONE/REJECTED |
| `bookings` | auto | customerUid, providerId, service, scheduledAt, status | status: NEW/CONFIRMED/DONE/CANCELLED |
| `appointments` | auto | customerUid, storeOrProviderId, purpose, scheduledAt, status | status: NEW/CONFIRMED/DONE/CANCELLED |
| `banners` | auto | imageUrl, title, target, active, order | sliding home banners |
| `categories` | auto | name, iconUrl, type | type: store / service |
| `growItems` | auto | title, description, imageUrl, ctaText, targetRole | admin-added; shown on seller "Grow your business" |
| `notifications` | auto | toUid, title, body, read, createdAt | per-user |

`*geohash` — add for radius search at scale (not in the base model; see README scaling notes).

## Required composite indexes
- `orders`: storeId == X, orderBy createdAt desc
- `serviceRequests`: providerId == X, orderBy createdAt desc
- `bookings`: providerId == X, orderBy scheduledAt desc
- `appointments`: storeOrProviderId == X, orderBy scheduledAt desc
- `stores` / `services`: approved == true, category == X
