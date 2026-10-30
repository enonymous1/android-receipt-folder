# Firebase: what v1 did, and what v2 deliberately does not

v1 (2023) used Firebase Auth for login/register and Firestore for storage.
Each signed-in user's receipts lived in a collection named after their UID,
which was sound. Signed-out users, however, all shared one collection
literally named "Trial Run", readable and writable by anyone running the
app without an account. The committed `google-services.json` also carried
a Firebase API key, and the app had no Firestore rules file in the repo
at all, so the project's live rules are unverified.

v2 is local-first on purpose:

- Receipts live in Room (`receipts.db`) on the device.
- Photos live in the app's private `files/receipts/` directory.
- OCR runs on-device with ML Kit. Nothing is uploaded.
- There is no login screen, no account, and no Firebase dependency,
  so the app builds in CI with no `google-services.json` and no secret
  to rotate or leak.

If cloud sync is ever wanted again, the safe shape is in
`docs/firestore.rules`: per-user paths (`users/{uid}/receipts/{id}`),
rules that require `request.auth.uid == userId`, Firebase Storage
uploads with the same per-user prefix, and the API key restricted in
the Google Cloud console to the app's package and SHA-1. The old
Firebase project's key and rules should be checked and rotated before
any public copy reuses that project, because the key was committed in
v1's history.
