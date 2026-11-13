# XSD BOOST — Final Game Booster Layout

## Requested primary layout
The intended five primary Game Booster sections are:
1. Perf — live device monitoring and supported maintenance actions.
2. Aim — visual crosshair overlay with position, size, thickness and style controls.
3. Sens — per-game X/Y profile values. These are saved profiles; Android does not expose a general API to rewrite another game's private sensitivity settings.
4. Music — local audio player and Dynamic Island controls.
5. Resolution — device display size/density controls only where the Android version, Shizuku authorization and device support the operation.

## Included source adjustments in this package
- Added a four-direction Crosshair Position Control pad. Each press adjusts normalized X/Y by 2%; the existing sliders remain available.
- Added a one-tap reset to the center position.
- Changed the default handle side to LEFT for newly created game profiles. Existing saved profiles retain their previously selected side.
- Dynamic Island already supports dragging its top grip and persists X/Y coordinates in `xsd_island_position` preferences.
- Sensitivity X/Y remains as a per-game profile control; it is not represented as direct game-memory/touch injection.
- Music Library and existing media controls are retained.

## Important implementation boundaries
- The current Dynamic Island position is adjusted by dragging the Island's grip. This package does not add separate directional position buttons inside the Game Booster settings panel.
- This source snapshot does not yet contain a verified, device-tested Resolution apply/restore screen. Do not assume that the existing graphics presets change Android display resolution.
- The crosshair is a visual overlay; it does not modify game aim, recoil, or internal game input.
- No APK build is claimed. Compile and test with GitHub Actions on the target device before distribution.
