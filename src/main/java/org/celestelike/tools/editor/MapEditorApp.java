package org.celestelike.tools.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.celestelike.game.logging.GameLogger;
import org.celestelike.game.world.LevelData;
import org.celestelike.game.world.LevelData.TileBlueprint;
import org.celestelike.game.world.TilesetIO;
import org.celestelike.game.world.TilesetIO.TilesetData;

/**
 * Lightweight in-engine map editor.
 *
 * <p>Layout:
 * <ul>
 *   <li>Left pane – grid view of the authored map (frames from the TSX tileset).</li>
 *   <li>Right pane – tile palette sliced from a TSX file using {@link TilesetIO}.</li>
 * </ul>
 *
 * <p>Controls (per-tile, after clicking a cell in the map view):
 * <ul>
 *   <li>Left click map: select cell and, if a palette tile is active, paint that tile index.</li>
 *   <li>Left click palette: select tile index for painting.</li>
 *   <li>Key 1: toggle SOLID flag for the selected cell.</li>
 *   <li>Key 2: toggle WATER flag for the selected cell.</li>
 *   <li>Key 3: toggle DOOR at the selected cell (door channel cycles 1..8).</li>
 *   <li>Key 4: toggle KEY at the selected cell (associated with the current door channel).</li>
 *   <li>Key 5: cycle ENEMY type for the selected cell (deathBoss, redDeon, skeletonEnemie).</li>
 *   <li>Ctrl+S: save blueprint + collision + enemies to JSON files read by the game.</li>
 *   <li>Ctrl+C: clear all metadata (solid/water/door/key/enemy) for the selected cell.</li>
 *   <li>ESC: quit editor.</li>
 * </ul>
 *
 * <p>Data files:
 * <ul>
 *   <li>{@code editor_blueprint.json} – maintained via {@link LevelData#saveBlueprint(TileBlueprint[][])}.</li>
 *   <li>{@code inspector_snapshot.json} – contains solid/water/doors/keys/enemies for collision & metadata.</li>
 *   <li>{@code enemy_spawns.json} – flat array of enemy spawn objects preferred by {@code EnemySpawnLoader}.</li>
 * </ul>
 */
public final class MapEditorApp extends ApplicationAdapter {

    private static final String SNAPSHOT_PATH = "inspector_snapshot.json";
    private static final String ENEMY_SPAWNS_PATH = "enemy_spawns.json";

    private static final String[] ENEMY_TYPES = {
            "deathBoss",
            "redDeon",
            "skeletonEnemie"
    };

    private final String tsxPath;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;

    private TileBlueprint[][] blueprint;

    // collision & metadata masks
    private boolean[][] solidMask;
    private boolean[][] waterMask;

    private final Map<Cell, DoorInfo> doors = new HashMap<>();
    private final Map<Cell, KeyInfo> keys = new HashMap<>();
    private final Map<Cell, String> enemies = new HashMap<>();

    private int rows;
    private int cols;

    private TilesetData tileset;
    private final List<Texture> tilesetTextures = new ArrayList<>();
    private final List<TextureRegion> tilesetRegions = new ArrayList<>();

    // layout in screen pixels
    private int screenHeight;
    private int mapPaneWidth;
    private int palettePaneWidth;
    private int tileSizePx;
    private int paletteTileSizePx;

    // selection state
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int selectedPaletteIndex = -1;
    private int currentDoorChannel = 1;

    public MapEditorApp(String tsxPath) {
        this.tsxPath = tsxPath;
    }

    @Override
    public void create() {
        camera = new OrthographicCamera();
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        loadBlueprint();
        loadSnapshot();
        loadEnemies();
        loadTileset();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        GameLogger.info("MapEditor started with TSX: " + tsxPath);
    }

    @Override
    public void resize(int width, int height) {
        this.screenHeight = height;

        // 70% for map, 30% for palette
        mapPaneWidth = (int) (width * 0.7f);
        palettePaneWidth = width - mapPaneWidth;

        // choose tile size so the whole map fits in the map pane
        if (cols > 0 && rows > 0) {
            int maxTileWidth = mapPaneWidth / cols;
            int maxTileHeight = height / rows;
            tileSizePx = Math.max(8, Math.min(maxTileWidth, maxTileHeight));
        } else {
            tileSizePx = 32;
        }

        // palette tile size: fit columns from tileset and rows it reports
        int paletteCols = Math.max(1, tileset != null ? tileset.columns() : 1);
        int paletteRows = Math.max(1, tileset != null ? tileset.rows() : 1);
        int maxPaletteWidth = palettePaneWidth / paletteCols;
        int maxPaletteHeight = height / paletteRows;
        paletteTileSizePx = Math.max(8, Math.min(maxPaletteWidth, maxPaletteHeight));

        camera.setToOrtho(false, width, height);
        camera.update();
    }

    @Override
    public void render() {
        handleInput();

        Gdx.gl.glClearColor(0.06f, 0.06f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        drawMapPane();
        drawPalettePane();
        drawOverlayText();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        if (tilesetTextures != null) {
            for (Texture texture : tilesetTextures) {
                texture.dispose();
            }
        }
        GameLogger.info("MapEditor terminated");
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    private void loadBlueprint() {
        blueprint = LevelData.copyBlueprint();
        rows = blueprint.length;
        cols = blueprint[0].length;
        solidMask = new boolean[rows][cols];
        waterMask = new boolean[rows][cols];
    }

    private void loadSnapshot() {
        if (Gdx.files == null) {
            return;
        }
        FileHandle handle = locate(SNAPSHOT_PATH);
        if (handle == null || !handle.exists()) {
            return;
        }
        try {
            JsonValue root = new JsonReader().parse(handle);
            JsonValue solidArray = root.get("solid");
            if (solidArray != null) {
                for (JsonValue entry : solidArray) {
                    if (entry.isArray() && entry.size >= 2) {
                        int r = entry.getInt(0);
                        int c = entry.getInt(1);
                        if (withinBounds(r, c)) {
                            solidMask[r][c] = true;
                        }
                    }
                }
            }
            JsonValue waterArray = root.get("water");
            if (waterArray != null) {
                for (JsonValue entry : waterArray) {
                    if (entry.isArray() && entry.size >= 2) {
                        int r = entry.getInt(0);
                        int c = entry.getInt(1);
                        if (withinBounds(r, c)) {
                            waterMask[r][c] = true;
                        }
                    }
                }
            }
            JsonValue doorsArray = root.get("doors");
            if (doorsArray != null) {
                for (JsonValue entry : doorsArray) {
                    if (!entry.isObject()) {
                        continue;
                    }
                    int r = entry.getInt("row", -1);
                    int c = entry.getInt("col", -1);
                    int channel = entry.getInt("channel", 1);
                    if (withinBounds(r, c)) {
                        doors.put(new Cell(r, c), new DoorInfo(channel));
                        currentDoorChannel = Math.max(currentDoorChannel, channel);
                    }
                }
            }
            JsonValue keysArray = root.get("keys");
            if (keysArray != null) {
                for (JsonValue entry : keysArray) {
                    if (!entry.isObject()) {
                        continue;
                    }
                    int r = entry.getInt("row", -1);
                    int c = entry.getInt("col", -1);
                    int channel = entry.getInt("channel", 1);
                    if (withinBounds(r, c)) {
                        keys.put(new Cell(r, c), new KeyInfo(channel));
                        currentDoorChannel = Math.max(currentDoorChannel, channel);
                    }
                }
            }
            JsonValue enemiesArray = root.get("enemies");
            if (enemiesArray != null) {
                for (JsonValue enemyValue : enemiesArray) {
                    if (!enemyValue.isObject()) {
                        continue;
                    }
                    String name = enemyValue.getString("name", null);
                    int r = enemyValue.getInt("row", -1);
                    int c = enemyValue.getInt("col", -1);
                    if (name != null && withinBounds(r, c)) {
                        enemies.put(new Cell(r, c), name);
                    }
                }
            }
        } catch (Exception exception) {
            Gdx.app.error("MapEditor", "Failed to parse " + SNAPSHOT_PATH, exception);
        }
    }

    private void loadEnemies() {
        if (Gdx.files == null) {
            return;
        }
        FileHandle enemyFile = locate(ENEMY_SPAWNS_PATH);
        if (enemyFile == null || !enemyFile.exists()) {
            return;
        }
        try {
            JsonValue root = new JsonReader().parse(enemyFile);
            if (!root.isArray()) {
                return;
            }
            for (JsonValue enemyValue : root) {
                if (!enemyValue.isObject()) {
                    continue;
                }
                String name = enemyValue.getString("name", null);
                int r = enemyValue.getInt("row", -1);
                int c = enemyValue.getInt("col", -1);
                if (name != null && withinBounds(r, c)) {
                    enemies.put(new Cell(r, c), name);
                }
            }
        } catch (Exception exception) {
            Gdx.app.error("MapEditor", "Failed to parse " + ENEMY_SPAWNS_PATH, exception);
        }
    }

    private void loadTileset() {
        tilesetTextures.clear();
        tilesetRegions.clear();

        if (tsxPath == null || tsxPath.isEmpty()) {
            Gdx.app.error("MapEditor", "No TSX path provided");
            return;
        }
        TilesetData data = TilesetIO.loadFromTsx(tsxPath);
        if (data.isEmpty()) {
            Gdx.app.error("MapEditor", "Failed to load tileset from " + tsxPath);
            return;
        }
        tileset = data;
        tilesetTextures.addAll(data.textures());
        tilesetRegions.addAll(data.regions());
    }

    private static FileHandle locate(String path) {
        FileHandle local = Gdx.files.local(path);
        if (local != null && local.exists()) {
            return local;
        }
        FileHandle internal = Gdx.files.internal(path);
        if (internal != null && internal.exists()) {
            return internal;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                saveAll();
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
                clearSelectionMetadata();
            }
        }

        if (Gdx.input.justTouched()) {
            handleMouseClick();
        }

        if (selectedRow >= 0 && selectedCol >= 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                toggleSolid(selectedRow, selectedCol);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                toggleWater(selectedRow, selectedCol);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                toggleDoor(selectedRow, selectedCol);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
                toggleKey(selectedRow, selectedCol);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
                cycleEnemy(selectedRow, selectedCol);
            }
        }
    }

    private void handleMouseClick() {
        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY() - 1;

        if (x < mapPaneWidth) {
            // click inside map
            int col = x / tileSizePx;
            int visualRow = y / tileSizePx;
            int row = rows - 1 - visualRow; // row 0 is the TOP row, same as runtime
            if (withinBounds(row, col)) {
                selectedRow = row;
                selectedCol = col;
                paintTileIfSelected(row, col);
                GameLogger.info("Selected map cell row=" + row + " col=" + col);
            }
        } else {
            // click inside palette
            int localX = x - mapPaneWidth;
            int col = localX / paletteTileSizePx;
            int row = y / paletteTileSizePx;
            int paletteCols = Math.max(1, tileset != null ? tileset.columns() : 1);
            int paletteIndex = row * paletteCols + col;
            if (paletteIndex >= 0 && paletteIndex < tilesetRegions.size()) {
                selectedPaletteIndex = paletteIndex;
                GameLogger.info("Selected palette index=" + paletteIndex);
            }
        }
    }

    private void paintTileIfSelected(int row, int col) {
        if (selectedPaletteIndex < 0 || selectedPaletteIndex >= tilesetRegions.size()) {
            return;
        }
        blueprint[row][col] = new TileBlueprint(new int[] {selectedPaletteIndex}, 0.15f);
    }

    private void toggleSolid(int row, int col) {
        solidMask[row][col] = !solidMask[row][col];
        GameLogger.info("Cell (" + row + "," + col + ") solid=" + solidMask[row][col]);
    }

    private void toggleWater(int row, int col) {
        waterMask[row][col] = !waterMask[row][col];
        GameLogger.info("Cell (" + row + "," + col + ") water=" + waterMask[row][col]);
    }

    private void toggleDoor(int row, int col) {
        Cell key = new Cell(row, col);
        DoorInfo existing = doors.get(key);
        if (existing != null) {
            doors.remove(key);
            GameLogger.info("Removed door at (" + row + "," + col + ")");
            return;
        }
        currentDoorChannel++;
        if (currentDoorChannel > 8) {
            currentDoorChannel = 1;
        }
        DoorInfo door = new DoorInfo(currentDoorChannel);
        doors.put(key, door);
        GameLogger.info(
                "Placed door at (" + row + "," + col + ") channel=" + door.channel());
    }

    private void toggleKey(int row, int col) {
        Cell keyCell = new Cell(row, col);
        KeyInfo existing = keys.get(keyCell);
        if (existing != null) {
            keys.remove(keyCell);
            GameLogger.info("Removed key at (" + row + "," + col + ")");
            return;
        }
        // Associate with current door channel
        int channel = MathUtils.clamp(currentDoorChannel, 1, 8);
        KeyInfo info = new KeyInfo(channel);
        keys.put(keyCell, info);
        GameLogger.info(
                "Placed key at (" + row + "," + col + ") channel=" + info.channel());
    }

    private void cycleEnemy(int row, int col) {
        Cell cell = new Cell(row, col);
        String current = enemies.get(cell);
        if (current == null) {
            enemies.put(cell, ENEMY_TYPES[0]);
            GameLogger.info("Placed enemy " + ENEMY_TYPES[0] + " at (" + row + "," + col + ")");
            return;
        }
        int index = 0;
        for (int i = 0; i < ENEMY_TYPES.length; i++) {
            if (ENEMY_TYPES[i].equals(current)) {
                index = i;
                break;
            }
        }
        int nextIndex = (index + 1) % ENEMY_TYPES.length;
        String next = ENEMY_TYPES[nextIndex];
        enemies.put(cell, next);
        GameLogger.info("Changed enemy at (" + row + "," + col + ") to " + next);
    }

    private void clearSelectionMetadata() {
        if (!withinBounds(selectedRow, selectedCol)) {
            return;
        }
        solidMask[selectedRow][selectedCol] = false;
        waterMask[selectedRow][selectedCol] = false;
        doors.remove(new Cell(selectedRow, selectedCol));
        keys.remove(new Cell(selectedRow, selectedCol));
        enemies.remove(new Cell(selectedRow, selectedCol));
        blueprint[selectedRow][selectedCol] = TileBlueprint.air();
        GameLogger.info("Cleared cell (" + selectedRow + "," + selectedCol + ")");
    }

    // -------------------------------------------------------------------------
    // Saving
    // -------------------------------------------------------------------------

    private void saveAll() {
        boolean blueprintOk = LevelData.saveBlueprint(blueprint);
        boolean snapshotOk = saveSnapshot();
        boolean enemiesOk = saveEnemySpawns();
        GameLogger.info("Editor save: blueprint=" + blueprintOk
                + " snapshot=" + snapshotOk
                + " enemies=" + enemiesOk);
    }

    private boolean saveSnapshot() {
        if (Gdx.files == null) {
            return false;
        }
        FileHandle handle = Gdx.files.local(SNAPSHOT_PATH);
        try (Writer writer = handle.writer(false, "UTF-8")) {
            JsonWriter json = new JsonWriter(writer);
            json.setOutputType(JsonWriter.OutputType.json);
            json.object();

            // solid cells
            json.name("solid").array();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (solidMask[r][c]) {
                        json.array().value(r).value(c).pop();
                    }
                }
            }
            json.pop(); // solid array

            // water cells
            json.name("water").array();
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (waterMask[r][c]) {
                        json.array().value(r).value(c).pop();
                    }
                }
            }
            json.pop(); // water array

            // doors
            json.name("doors").array();
            for (Map.Entry<Cell, DoorInfo> entry : doors.entrySet()) {
                Cell cell = entry.getKey();
                DoorInfo door = entry.getValue();
                json.object();
                json.name("row").value(cell.row());
                json.name("col").value(cell.col());
                json.name("channel").value(door.channel());
                json.pop();
            }
            json.pop(); // doors array

            // keys
            json.name("keys").array();
            for (Map.Entry<Cell, KeyInfo> entry : keys.entrySet()) {
                Cell cell = entry.getKey();
                KeyInfo info = entry.getValue();
                json.object();
                json.name("row").value(cell.row());
                json.name("col").value(cell.col());
                json.name("channel").value(info.channel());
                json.pop();
            }
            json.pop(); // keys array

            // enemies – keep for backwards compatibility
            json.name("enemies").array();
            for (Map.Entry<Cell, String> entry : enemies.entrySet()) {
                Cell cell = entry.getKey();
                json.object();
                json.name("name").value(entry.getValue());
                json.name("row").value(cell.row());
                json.name("col").value(cell.col());
                json.pop();
            }
            json.pop(); // enemies array

            json.pop(); // root object
            json.close();
            return true;
        } catch (IOException exception) {
            Gdx.app.error("MapEditor", "Failed to save " + SNAPSHOT_PATH, exception);
            return false;
        }
    }

    private boolean saveEnemySpawns() {
        if (Gdx.files == null) {
            return false;
        }
        FileHandle handle = Gdx.files.local(ENEMY_SPAWNS_PATH);
        try (Writer writer = handle.writer(false, "UTF-8")) {
            JsonWriter json = new JsonWriter(writer);
            json.setOutputType(JsonWriter.OutputType.json);
            json.array();
            for (Map.Entry<Cell, String> entry : enemies.entrySet()) {
                Cell cell = entry.getKey();
                json.object();
                json.name("name").value(entry.getValue());
                json.name("row").value(cell.row());
                json.name("col").value(cell.col());
                json.pop();
            }
            json.pop(); // root array
            json.close();
            return true;
        } catch (IOException exception) {
            Gdx.app.error("MapEditor", "Failed to save " + ENEMY_SPAWNS_PATH, exception);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void drawMapPane() {
        if (blueprint == null) {
            return;
        }

        batch.begin();
        for (int r = 0; r < rows; r++) {
            int visualRow = rows - 1 - r; // row 0 (top) drawn at highest Y
            for (int c = 0; c < cols; c++) {
                TileBlueprint cell = blueprint[r][c];
                if (cell == null || cell.frames().length == 0) {
                    continue;
                }
                int index = cell.frames()[0];
                if (index < 0 || index >= tilesetRegions.size()) {
                    continue;
                }
                TextureRegion region = tilesetRegions.get(index);
                float x = c * tileSizePx;
                float y = visualRow * tileSizePx;
                batch.draw(region, x, y, tileSizePx, tileSizePx);
            }
        }
        batch.end();

        // grid, selection & masks
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.15f, 0.15f, 0.2f, 1f);
        for (int r = 0; r <= rows; r++) {
            float y = r * tileSizePx;
            shapes.line(0, y, mapPaneWidth, y);
        }
        for (int c = 0; c <= cols; c++) {
            float x = c * tileSizePx;
            shapes.line(x, 0, x, screenHeight);
        }

        // highlight solid/water/door/key/enemy
        for (int r = 0; r < rows; r++) {
            int visualRow = rows - 1 - r;
            for (int c = 0; c < cols; c++) {
                float x = c * tileSizePx;
                float y = visualRow * tileSizePx;
                Cell cell = new Cell(r, c);

                if (solidMask[r][c]) {
                    shapes.setColor(0f, 1f, 0f, 0.75f);
                    shapes.rect(x + 1, y + 1, tileSizePx - 2, tileSizePx - 2);
                }
                if (waterMask[r][c]) {
                    shapes.setColor(0f, 0.5f, 1f, 0.75f);
                    shapes.rect(x + 4, y + 4, tileSizePx - 8, tileSizePx - 8);
                }
                if (doors.containsKey(cell)) {
                    shapes.setColor(1f, 1f, 0f, 0.85f);
                    shapes.rect(x + 2, y + 2, tileSizePx - 4, tileSizePx - 4);
                }
                if (keys.containsKey(cell)) {
                    shapes.setColor(1f, 0.5f, 0f, 0.85f);
                    shapes.rect(x + tileSizePx / 4f, y + tileSizePx / 4f,
                            tileSizePx / 2f, tileSizePx / 2f);
                }
                if (enemies.containsKey(cell)) {
                    shapes.setColor(1f, 0f, 0f, 0.85f);
                    shapes.rect(x + tileSizePx / 3f, y + tileSizePx / 3f,
                            tileSizePx / 3f, tileSizePx / 3f);
                }
            }
        }

        // selected cell outline
        if (withinBounds(selectedRow, selectedCol)) {
            shapes.setColor(Color.WHITE);
            int visualRow = rows - 1 - selectedRow;
            float x = selectedCol * tileSizePx;
            float y = visualRow * tileSizePx;
            shapes.rect(x, y, tileSizePx, tileSizePx);
        }

        shapes.end();
    }

    private void drawPalettePane() {
        if (tilesetRegions.isEmpty()) {
            return;
        }
        int offsetX = mapPaneWidth;

        int paletteCols = Math.max(1, tileset != null ? tileset.columns() : 1);

        batch.begin();
        for (int i = 0; i < tilesetRegions.size(); i++) {
            TextureRegion region = tilesetRegions.get(i);
            int col = i % paletteCols;
            int row = i / paletteCols;
            float x = offsetX + col * paletteTileSizePx;
            float y = row * paletteTileSizePx;
            batch.draw(region, x, y, paletteTileSizePx, paletteTileSizePx);
        }
        batch.end();

        // grid & selection
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.15f, 0.15f, 0.2f, 1f);

        int rowsNeeded = (int) Math.ceil(tilesetRegions.size()
                / (float) paletteCols);
        for (int r = 0; r <= rowsNeeded; r++) {
            float y = r * paletteTileSizePx;
            shapes.line(offsetX, y, offsetX + palettePaneWidth, y);
        }
        for (int c = 0; c <= paletteCols; c++) {
            float x = offsetX + c * paletteTileSizePx;
            shapes.line(x, 0, x, screenHeight);
        }

        if (selectedPaletteIndex >= 0
                && selectedPaletteIndex < tilesetRegions.size()) {
            int col = selectedPaletteIndex % paletteCols;
            int row = selectedPaletteIndex / paletteCols;
            float x = offsetX + col * paletteTileSizePx;
            float y = row * paletteTileSizePx;
            shapes.setColor(Color.WHITE);
            shapes.rect(x, y, paletteTileSizePx, paletteTileSizePx);
        }

        shapes.end();
    }

    private void drawOverlayText() {
        batch.begin();
        String status = "TSX: " + tsxPath
                + " | Selected cell: "
                + (withinBounds(selectedRow, selectedCol)
                    ? (selectedRow + "," + selectedCol)
                    : "none")
                + " | Selected tile index: "
                + (selectedPaletteIndex >= 0 ? selectedPaletteIndex : "none")
                + " | Door channel: " + currentDoorChannel;
        font.draw(batch, status, 8, screenHeight - 8);

        String help = "Mouse: LMB select/paint  |  1=solid  2=water  3=door  4=key  5=enemy cycle"
                + "  |  Ctrl+S=save  Ctrl+C=clear cell  Esc=quit";
        font.draw(batch, help, 8, screenHeight - 28);

        batch.end();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean withinBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private record Cell(int row, int col) {}

    private record DoorInfo(int channel) {}

    private record KeyInfo(int channel) {}
}


