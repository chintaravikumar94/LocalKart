# Razorpay setup — Seller activation & monthly subscription

The seller web app lets shop owners / service providers pay their **one-time activation fee**
and **monthly subscription** online. The flow is secure: the secret key lives only in Cloud
Functions, amounts are computed server-side from the admin's **Plans**, and the payment
signature is verified before any billing record is written.

```
Seller clicks Pay
   → createRazorpayOrder (Cloud Function)   # looks up plan, makes a Razorpay order
   → Razorpay Checkout opens (UPI/cards/netbanking)
   → verifyRazorpayPayment (Cloud Function) # verifies signature, credits billing/{uid} + payments
   → razorpayWebhook (Cloud Function)       # backup credit if the browser closes
```

Payments appear automatically in **Admin → Billing → Subscriptions** (same `billing` + `payments`
collections the admin already uses), tagged `via: "razorpay"`.

## 1. Get your Razorpay keys
1. Create an account at https://razorpay.com and complete KYC for live mode.
2. Dashboard → **Settings → API Keys → Generate Key**.
   - Test mode keys look like `rzp_test_xxxxxxxx` + a secret.
   - Live keys look like `rzp_live_xxxxxxxx` + a secret.

## 2. Give the keys to Cloud Functions
A Blaze (pay-as-you-go) plan is required to deploy functions and to call external APIs.

**Option A — functions config (simplest):**
```bash
cd C:\Projects\LocalKart
firebase functions:config:set ^
  razorpay.key_id="rzp_test_xxxxxxxx" ^
  razorpay.key_secret="YOUR_SECRET" ^
  razorpay.webhook_secret="ANY_RANDOM_STRING"
```
(`^` is the Windows line-continuation; or put it all on one line.)

**Option B — env file:** create `functions/.env` with
```
RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
RAZORPAY_KEY_SECRET=YOUR_SECRET
RAZORPAY_WEBHOOK_SECRET=ANY_RANDOM_STRING
```

## 3. Deploy the functions
```bash
firebase deploy --only functions
```
This deploys `createRazorpayOrder`, `verifyRazorpayPayment`, and `razorpayWebhook`
(plus the existing push functions).

## 4. (Recommended) Add the webhook
In Razorpay Dashboard → **Settings → Webhooks → Add New Webhook**:
- **URL:** `https://us-central1-localkart-7dfb4.cloudfunctions.net/razorpayWebhook`
- **Secret:** the same string you used for `webhook_secret`
- **Active events:** `payment.captured`, `order.paid`

This guarantees the subscription is credited even if the seller closes the tab right after paying.

## 5. Deploy the seller site
```bash
firebase deploy --only hosting:seller
```
Make sure `localkart-seller.web.app` is in **Authentication → Settings → Authorized domains**.

## Testing (test mode)
Use Razorpay's test UPI `success@razorpay` or test card `4111 1111 1111 1111`, any future expiry, any CVV.
After a successful test payment, the seller's **Billing** page flips to *Subscription active* and the
payment shows in the admin console.

## Notes
- Amounts are always taken from the **admin Plans** for the seller's category — never from the browser.
- One-time activation sets `activationPaid`; monthly payments extend `nextDueAt` by one month.
- To go live, swap the test keys for live keys (repeat step 2) and redeploy functions.
