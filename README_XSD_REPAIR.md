# XSD BOOST — source repair notes

## Package identity
- Android application ID: `app.xsdboostsystem`
- Kotlin namespace remains `com.example` to preserve the existing source package, generated `R` references, and component imports.
- Shizuku provider authority remains `${applicationId}.shizuku`, so it follows the new application ID automatically.

## Music status
- Existing Media3 engine handles direct audio/HLS streams.
- Existing YouTube handling is an external-app handoff, not embedded playback.
- Local document-picker integration was not found in the supplied source at inspection time. Do not advertise local-file playback as implemented until a SAF picker, persisted URI permission, and Media3 URI loading are connected.
- YouTube playback must use the official YouTube IFrame Player API/WebView integration and comply with YouTube terms. Do not extract stream URLs or bypass playback restrictions.

## Sensitivity status
- Existing X/Y values and game profiles are local settings.
- `InputCompatibilityLayer` explicitly reports that it cannot alter a game's internal touch/camera sensitivity through public Android APIs.
- The visual calibration pad is a local simulation only. Do not report these values as applied to Free Fire.
- Shizuku does not grant permission to inject touch events into or modify another game's process.

## Shizuku and Rish
- Existing Shizuku API/Binder/UserService implementation is retained.
- Supplied `rish` and `rish_shizuku.dex` are packaged under `app/src/main/assets/rish/` for reference/integration.
- These files are NOT automatically installed or executed. The supplied `rish` script contains a `PKG` placeholder intended for a terminal application's package ID; it must not be blindly run as the XSD BOOST package.
- Use Shizuku API/UserService as the primary integration. Rish is optional and must only execute validated, allowlisted operations.

## Build verification
This archive does not include `gradlew`, `gradlew.bat`, or a wrapper JAR, and the execution environment used for this repair did not have Gradle installed. Therefore an Android build could not be run here. Open the project in Android Studio and sync with its configured Gradle/AGP versions before producing an APK.
