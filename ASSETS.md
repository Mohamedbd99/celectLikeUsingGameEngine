## Kenney Pico-8 Platformer assets

- Source pack: `kenney_pico-8-platformer` (CC0 license by Kenney), mirrored inside `src/main/resources/assets/kenney_pico-8-platformer`.
- Tiles are 8×8 pixels with 1px spacing; the game renders them at `TILE_SCALE = 4` (32 px world units).
- `CelesteGame` now exposes a built-in editor:
  - A palette (bottom-right) lists every sprite from `Transparent/Tilemap/tilemap.png`. Click to pick a tile; X switches to air.
  - `[ / ]` change the number of frames painted at once (up to 3 contiguous tiles). `O` appends the currently selected tile as an extra frame on the hovered cell (non-contiguous animations supported).
  - `- / +` adjust frame timing (0.05s–0.50s). LMB paints, RMB clears, `C` clears everything, `P` prints Java code (see below).
- Export format: `P` prints `new TileBlueprint(new int[]{...}, duration)` entries that can be pasted into `LevelData`.
  - Frame arrays contain the exact palette indices in the playback order (supports mixed tiles).
  - `frameDuration` is in seconds; the editor preserves your per-cell timing.
- `LevelData` now stores a blueprint (`TileBlueprint[][]`) with explicit frame arrays. Replace it with the printed structure to bake in new layouts/animations.
- To use a different sprite for a specific behavior, just paint that sprite (optionally add up to 3 frames via `O`) and copy the emitted blueprint; no code changes are required unless you add custom logic for that tile id.

