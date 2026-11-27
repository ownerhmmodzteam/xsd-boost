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

## Sensitivity X/Y status
- Sensitivity X/Y controls, presets, simulation, callbacks, preference keys, and compatibility code have been removed from active app functionality.
- Inert Room schema columns are retained solely to avoid wiping existing game profiles during an upgrade; no runtime code reads or writes them.

## Shizuku
- Shizuku API/Binder/UserService and the AIDL interface are compiled into the application's DEX as part of the normal Android build.
- The external Rish script and its standalone DEX are not required by this integration and are removed from the APK assets.
- UserService command execution accepts only exact allowlisted argument sets; arbitrary shell strings are rejected.
- The supported actions are fixed-performance mode and Android package cache trimming. Device support and permission state can still affect the result.
- Touch/display tuning is not exposed as a working action because the previous command was incomplete and could not reliably apply a setting.

## Build verification
This archive does not include `gradlew`, `gradlew.bat`, or a wrapper JAR, and the execution environment used for this repair did not have Gradle installed. Therefore an Android build could not be run here. Open the project in Android Studio and sync with its configured Gradle/AGP versions before producing an APK.
