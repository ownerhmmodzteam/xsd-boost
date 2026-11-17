# XSD BOOST Game Space update

- Replaced the Home dashboard with an original dark/neon Game Space layout.
- Added an MP4 background player with looping, in-app video selection, and independent video volume.
- Added `app/src/main/assets/game_backgrounds/` for bundled landscape MP4 backgrounds.
- Added live device metrics cards and a ROG-style floating monitor toggle using the existing Android overlay service.
- Added Smart Boost shortcut using the existing memory analysis/cache maintenance workflow.
- Replaced the legacy Games tab with Home > Game Space. The picker lists installed games; the preview starts empty with 0 selected games until the user adds titles.
- Added a + control at the upper-left of Game Preview to open the game picker.
- Added crosshair D-pad position controls in Tools and the in-game Aim panel.
- Removed the active Sensitivity X/Y feature end-to-end: overlay controls, presets, callbacks, ViewModel actions, preferences, compatibility simulator, and legacy Game Center screen. Only inert Room schema fields remain to avoid destructive user-data migration.
- Kept the existing Dynamic Island music player as the app-wide music control surface.

## Add your videos
Put landscape MP4 files in `app/src/main/assets/game_backgrounds/`. Recommended H.264, 16:9 or 21:9, 720p/1080p. The screen also supports choosing a video from device storage.

## Technical notes
- FPS shown in the Game Space card is the app's available FPS estimate, not guaranteed per-game FPS.
- Android restricts third-party apps from force-clearing another app's memory. Smart Boost uses the existing supported cleanup workflow.
- Floating metrics require Android overlay permission and may be limited by device/vendor background restrictions.
