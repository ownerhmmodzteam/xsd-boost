# XSD BOOST - Dynamic Island Position

## What's new
- Drag the small horizontal grip at the top of the floating Music Dynamic Island to move it.
- The position is saved locally in SharedPreferences and restored when the overlay is attached again.
- The island uses top/start gravity and clamps movement to the visible screen bounds.
- Tapping the island body still expands/collapses it; playback controls remain independently clickable.

## Notes
- Android overlay permission must be granted for the floating island to appear.
- Position is stored per app installation on the current device.
- Build verification must be run in GitHub Actions; no APK build is claimed by this source package.


### Directional position controller (latest update)
Open Game Booster > Music > POSISI DYNAMIC ISLAND. Use the arrow buttons to move the island by 12 dp per tap. The center readout shows the current X and Y coordinates in dp. The selected position is saved automatically and constrained to the visible screen area.
