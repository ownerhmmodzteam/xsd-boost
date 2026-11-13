# XSD BOOST update notes

## Included in this package
- Added a short branded Compose splash screen before the main interface.
- Removed the entire `tools/` directory, including `.sh` and `.bat` helper scripts, as requested.
- Preserved the existing overlay implementation and its persisted size/opacity preferences.
- Preserved the existing local music library and Media3 playback implementation.

## YouTube Music integration status
The project currently uses an official-app handoff for YouTube links/IDs. Public open-source Android projects found during research use YouTube Music's undocumented/private InnerTube interfaces for direct stream resolution. These are not stable or official playback APIs and may break or conflict with service terms. This package therefore does not embed an extractor or claim direct audio-only YouTube playback. A YouTube ID can be opened in the installed official YouTube app; no thumbnail or embedded player is required in XSD BOOST.

## Not claimed as completed
This source package has not been compiled in this environment. Shizuku behavior, actual device telemetry, all Settings actions, and Android process lifecycle still require device/build verification. Existing telemetry values are not relabeled as actual if the underlying platform does not expose them.
- Simplified the Monitor overlay settings UI: the built-in overlay handle exposes only size and opacity controls. The previous side/hidden/long-press customization controls are no longer presented as user customization options.
