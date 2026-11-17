# XSD BOOST — Final Game Booster Layout

## Requested primary layout
The intended five primary Game Booster sections are:
1. Perf — live device monitoring and supported maintenance actions.
2. Aim — visual crosshair overlay with position, size, thickness and style controls.
4. Music — local audio player and Dynamic Island controls.
5. Resolution — device display size/density controls only where the Android version, Shizuku authorization and device support the operation.

## Included source adjustments in this package
- Added a four-direction Crosshair Position Control pad in both Tools and the in-game Aim overlay. Each press adjusts normalized position by 2%.
- Added a one-tap reset to the center position.
- Changed the default handle side to LEFT for newly created game profiles. Existing saved profiles retain their previously selected side.
- Dynamic Island already supports dragging its top grip and persists X/Y coordinates in `xsd_island_position` preferences.
- Sensitivity X/Y has been removed from active functionality, including overlay UI, game profiles, presets, and ViewModel actions. Legacy Room columns are inert and retained only for database compatibility.
- Music Library and existing media controls are retained.

## Important implementation boundaries
- The Dynamic Island keeps its existing draggable position control; the Crosshair uses its own four-direction pad.
- This source snapshot does not yet contain a verified, device-tested Resolution apply/restore screen. Do not assume that the existing graphics presets change Android display resolution.
- The crosshair is a visual overlay; it does not modify game aim, recoil, or internal game input.
- No APK build is claimed. Compile and test with GitHub Actions on the target device before distribution.
