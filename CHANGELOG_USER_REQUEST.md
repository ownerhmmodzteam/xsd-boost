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


## Final Game Booster layout request (2026-11-13)
- Added directional Crosshair position controls and center reset alongside X/Y sliders.
- New game profiles now default the edge handle to the LEFT side. Existing profiles are not forcibly overwritten.
- Retained the five requested priorities as the product direction: Perf, Aim, Sensitivity X/Y, Music, Resolution.
- See README_FINAL_GAME_BOOSTER_LAYOUT.md for the exact implementation status and platform limitations.


## Dynamic Island directional position controls
- Added a D-pad style position controller inside Game Booster > Music.
- Up/down/left/right buttons move the floating Dynamic Island in 12 dp increments.
- Center readout displays current X/Y position in dp.
- Position is clamped to screen bounds and persisted in `xsd_island_position` preferences.

## Animated splash screen update
- Replaced the static text-only splash with an animated XSD BOOST mascot splash.
- Added a soft neon-green radial glow, breathing logo scale, fade/scale entrance, and animated loading indicator.
- Splash now stays visible for approximately 2.8 seconds before entering the app.
- Uses the existing transparent `xsd_brand_anime.png` resource; no external runtime asset download is required.
