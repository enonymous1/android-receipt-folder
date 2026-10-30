# Changelog

## 2.0.1 — 2026-10-30

Fixes the first CI run, which failed in 42s at project configuration,
before any code compiled. No app code changed.

- Added the Room Gradle plugin's required `room { schemaDirectory(...) }`
  block to `app/build.gradle.kts`, pointing at `app/schemas`. Room 2.7
  fails configuration outright without it, even though this database
  sets `exportSchema = false` and exports nothing. The failure was a
  build-configuration verdict, not a verdict on the app code or tests.
- Bumped `versionName` to 2.0.1.

## 2.0.0 — 2026-10-30

Standalone modernization of the October 2023 Java/Views project
(`justin-lyons-2310`, "Receipt-Folder"). Rewritten, not patched.

### Added

- Kotlin + Jetpack Compose Material 3 UI, single activity, Navigation.
- ViewModel + StateFlow over a Room database (`receipts.db`).
- CameraX capture saving full-resolution photos to private files.
- Gallery pick via `GetContent`, copied into private files.
- On-device ML Kit OCR parsed into merchant, date, and total
  (`ReceiptParser`, unit-tested).
- Working edit screen, delete confirmation, and a real paginated PDF
  report with count and grand total, shareable via FileProvider.
- Version catalog, Kotlin DSL build scripts, R8 + resource shrinking
  on release, MIT LICENSE, GitHub Actions CI.

### Changed

- Money is integer cents, not `float`; dates are epoch millis, not
  free text; receipts are keyed by id in the list.
- Only the CAMERA permission is requested. The old
  READ/WRITE_EXTERNAL_STORAGE declarations are gone (unusable under
  scoped storage); photos and reports live in private files.
- SDK: compile/target 36, minSdk 24, AGP 8.13.0, Gradle 8.14.3,
  Java 17, Kotlin 2.2.0, Compose BOM 2025.10.01.
- Package and app id: `com.enonymous1.receiptfolder`; app name
  "Receipt Folder". The 2.0 MB BMP logo asset is removed; the launcher
  icon is a vector receipt.
- Firebase Auth and Firestore are removed in favour of local Room
  storage. See `docs/firebase-migration.md` for why, and for the safe
  shape of any future cloud sync.

### Removed

- The seven Activity+Fragment pairs, the per-fragment lists, the
  shared "Trial Run" Firestore collection, the trial/test login, and
  the template-only unit and instrumented tests.

### Fixed (v1 bugs, from its own README and code)

- Captured images no longer vanish (full-resolution save, not the
  camera intent's thumbnail).
- OCR text no longer lands whole in the merchant field.
- List no longer duplicates rows on snapshot re-emissions.
- Edit is no longer a no-op; viewer now actually shows the photo.
- PDF report is no longer a 100×100 page saying only "RECEIPT".
- Auth failures are no longer silently ignored — there is no auth
  to fail in v2.
- `local.properties`, `google-services.json`, `.idea/`, and CRLF
  line endings are not in this export; `gradlew` is executable.
