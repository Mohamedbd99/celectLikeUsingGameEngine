## Kenney Pico-8 Platformer assets

- Source pack: `kenney_pico-8-platformer` (CC0 license by Kenney), mirrored inside `src/main/resources/assets/kenney_pico-8-platformer`.
- Tiles are 8×8 pixels with 1px spacing; the game renders them at `TILE_SCALE = 4` (32 px world units).
- `ViewEditor` hosts the in-game editor (launch via `org.example.desktop.EditorLauncher`):
  - A palette overlay lists every sprite from `Transparent/Tiles`. Click to pick a tile; X switches to air. Drag the top bar to reposition it anywhere on screen.
  - `[ / ]` change the number of frames painted at once (up to 3 contiguous tiles). `O` appends the currently selected tile as an extra frame on the hovered cell (non-contiguous animations supported).
  - `- / +` adjust frame timing (0.05s–0.50s). LMB paints, RMB clears, `C` clears everything, `P` prints Java code (see below).
  - Hold `Shift` while clicking to place/remove entity markers (F1 none, F2 enemy, F3 grandpa/NPC, F4 key, F5 door). For keys/doors, adjust the shared channel with F6/F7 to wire keys to matching doors.
- Export format: `P` prints `new TileBlueprint(new int[]{...}, duration)` entries that can be pasted into `LevelData`.
  - Frame arrays contain the exact palette indices in the playback order (supports mixed tiles).
  - `frameDuration` is in seconds; the editor preserves your per-cell timing.
- `LevelData` now stores a blueprint (`TileBlueprint[][]`) with explicit frame arrays. Replace it with the printed structure to bake in new layouts/animations.
- `CelesteGame` (booted by `org.example.desktop.DesktopLauncher`) is a lightweight runtime that just renders the baked blueprint. The editor is completely separate—run `EditorLauncher` only when you want to edit.
- To use a different sprite for a specific behavior, just paint that sprite (optionally add up to 3 frames via `O`) and copy the emitted blueprint; no code changes are required unless you add custom logic for that tile id.

