# Push Notifications — setup

The **apps already receive** push notifications (a messaging service, notification channel, runtime permission, and FCM-token saving are all built in). What's left is the **server piece** that actually sends a push when something happens — that runs as a Cloud Function.

## How it works
1. On login, each app saves its FCM token to `users/{uid}.fcmToken`.
2. When a customer creates an order / booking / service request, a Cloud Function fires, looks up the seller who owns that store/service, reads their token, and sends the push.
3. The seller's device shows the notification (even in the background).

## Deploy the Cloud Functions (one time)

Requires the **Blaze plan** (you're already on it) and Node.js installed.

```powershell
# 1. Install the Firebase CLI
npm install -g firebase-tools

# 2. Log in
firebase login

# 3. From the project root, link this folder to your project
cd C:\Projects\LocalKart
firebase use --add        # pick localkart-7dfb4

# 4. Install function deps
cd functions
npm install
cd ..

# 5. Deploy
firebase deploy --only functions
```

If `firebase use --add` asks for a `firebase.json`, create one in the project root:
```json
{ "functions": { "source": "functions" } }
```

After deploy, the three triggers (`onNewOrder`, `onNewBooking`, `onNewServiceRequest`) are live.

## Test
1. Run the **seller app**, sign in (so its token is saved), then background the app.
2. In the **customer app** (same store's owner account, or a real customer), place an order / book / request.
3. The seller device should get a push within a few seconds.

> Quick manual test without deploying: Firebase Console → **Messaging → Send test message** → paste a device's FCM token (log it from `PushTokens` or read `users/{uid}.fcmToken` in Firestore).

## Notes
- Foreground messages are shown by `LocalKartMessagingService.onMessageReceived`; background messages with a `notification` payload are shown by the system automatically.
- For production, send to topics or batch tokens, and remove stale tokens on send failure.
