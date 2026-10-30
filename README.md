# Receipt Folder

An Android app for photographing receipts, reading them on-device, and
keeping them organised — merchant, date, and total — with a real PDF
report. Local-first: receipts and photos stay on the device.

## Origin

Modernized from a Full Sail Project and Portfolio V Android project
(October 2023), originally written in Java with Views, Firebase Auth,
and Firestore. The original only ever saved two fields per receipt
(`id` and `merchant`); OCR text was dumped whole into the merchant
field, the photo was never uploaded, the edit action was a no-op, and
the "report" was a 100×100 PDF containing only the word "RECEIPT".

v2 rewrites the app rather than patching it, and fixes each of those
by construction. See [CHANGELOG.md](CHANGELOG.md) for the full list.

## What it does

- **Capture** — CameraX preview that saves a full-resolution photo to
  the app's private files (v1's camera intent returned only a
  thumbnail, which is why captured images vanished). Gallery pick is
  supported too, with no storage permission needed.
- **Read** — On-device ML Kit text recognition, parsed into merchant
  (first line), date (MM/DD/YYYY, YYYY-MM-DD, or month-name dates),
  and total (the amount on the last "total" line, else the largest
  amount). The form always shows the parsed values for correction
  before saving.
- **Keep** — Room database, one row per receipt, money as integer
  cents (never a float), dates as epoch millis. The list is a keyed
  `LazyColumn` over a single Room `Flow`, so it cannot duplicate rows
  the way v1's per-fragment snapshot listener did.
- **View and edit** — Detail screen with the photo, merchant, address,
  date, and total; edit works; delete asks first.
- **Report** — A real PDF: one line per receipt, a count, and a grand
  total, paginated and shareable through FileProvider.

There is no account and no cloud sync in v2. That is deliberate: it
removes v1's shared "Trial Run" Firestore collection, in which every
signed-out user wrote to the same place. See
[docs/firebase-migration.md](docs/firebase-migration.md) for the safe
shape if cloud sync is ever re-added.

## Stack

Kotlin 2.2.0, Jetpack Compose Material 3, Navigation, ViewModel +
StateFlow, Room 2.7.2, CameraX 1.3.4, ML Kit text recognition 16.0.0,
AGP 8.13.0, Gradle 8.14.3, Java 17, compile/target SDK 36, minSdk 24.

## Build and run

Open in Android Studio (Ladybug or later) and run on a device or
emulator with a camera, or from the command line:

```sh
./gradlew testDebugUnitTest assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.
A physical device is the honest test for the camera and OCR paths;
the emulator camera works with a virtual scene image.

## Tests

18 unit tests, run by `./gradlew testDebugUnitTest` and by CI:

- `ReceiptParserTest` (11) — merchant, three date formats, total-line
  priority, largest-amount fallback, bare numbers, empty and invalid input.
- `MoneyAndValidationTest` (4) — currency formatting, blank merchant,
  negative total.
- `ReceiptViewModelTest` (3) — save, delete, and pending-capture
  handling against an in-memory fake repository.

There is one instrumented smoke test (package name). Instrumented
tests are not run in CI, which has no emulator; run them from Android
Studio on a device.

## Project layout

```text
app/src/main/java/com/enonymous1/receiptfolder/
  MainActivity.kt            Single activity, Room + ViewModel wiring
  data/ReceiptData.kt        Room entity, DAO, database, repository
  domain/ReceiptParser.kt    OCR text -> merchant / date / total
  domain/Money.kt            Cents and date formatting
  ui/ReceiptApp.kt           Navigation graph
  ui/Screens.kt              Home, list, detail, form
  ui/CaptureScreen.kt        CameraX capture + gallery + OCR
  ui/ReportsScreen.kt        PDF report + share
  ui/ReceiptViewModel.kt     Shared StateFlow + pending capture
  ui/OcrHelper.kt            ML Kit recognition, image copying
  ui/theme/Theme.kt          Material 3 theme
```

## Known limits

- OCR parsing is heuristic: unusual receipt layouts will need a manual
  correction on the form, which is why the form is always shown first.
- No cloud backup in v2: receipts live only on the device, so a device
  loss loses them. Android Auto Backup covers the app's private files
  unless the user has it disabled.
- No live build has been run on this machine (no JDK/Android SDK
  here); the gate is GitHub Actions CI plus Android Studio on a
  Windows box, including a real-device capture and OCR pass.

## License

MIT. See [LICENSE](LICENSE).
