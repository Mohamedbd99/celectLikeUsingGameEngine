package org.celestelike.tools.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.celestelike.game.world.LevelData;
import org.celestelike.game.world.LevelData.TileBlueprint;
import org.celestelike.game.world.TilesetIO;
import org.celestelike.game.world.TilesetIO.TilesetData;

/**
 * Tile editor for the Kenney Pico-8 tileset that lets you pick any sprite and
 * configure simple horizontal animations directly inside the running game.
 */
public class ViewEditor extends ApplicationAdapter {

    private static final String TILESET_BASE = "assets/newTileSetManara/";
    private static final String TILESET_TSX = "assets/b.tsx";
    private static final float TILE_SCALE = 8f;          // 8px -> 32px world units
    private static final float PALETTE_SCALE = 4f;       // palette tiles drawn at 32px
    private static final float PALETTE_MARGIN = 32f;
    private static final float PALETTE_HEADER_HEIGHT = 40f;
    private static final float MIN_FRAME_DURATION = 0.05f;
    private static final float MAX_FRAME_DURATION = 0.5f;
    private static final float FRAME_DURATION_STEP = 0.02f;
    private final List<Texture> paletteTextures = new ArrayList<>();
    private final List<TextureRegion> paletteRegions = new ArrayList<>();
    private final PaletteSharedState paletteBridge = new PaletteSharedState();
    private int paletteRows;
    private int paletteCols = LevelData.ATLAS_COLUMNS;
    private float paletteTileSize = LevelData.TILE_SIZE * PALETTE_SCALE;
    private boolean paletteDetached = true;
    private boolean paletteWindowSpawned;
    
    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;


    private TileCell[][] cells;
    private EntityMarker[][] entities;

    // Simple collision brush types so one click can assign BOTH tile art and collision.
    private enum CollisionType {
        NONE,
        SOLID,
        WATER,
        DOOR,
        KEY
    }

    private CollisionType[][] collision;
    private CollisionType collisionBrush = CollisionType.NONE;
    private EntityType entityBrush = EntityType.NONE;
    private int entityChannel = 0;
    private float tileWorldSize;
    private float worldWidth;
    private float worldHeight;
    private float elapsed;

    private int hoverRow = -1;
    private int hoverCol = -1;
    private boolean pointerOverPalette;
    private int paletteHoverIndex = -1;
    private boolean draggingPalette;
    private float dragOffsetX;
    private float dragOffsetY;

    private int selectedTileIndex = -1;
    private int selectedFrameCount = 1;
    private float selectedFrameDuration = 0.15f;
    private static final float STATUS_MESSAGE_DURATION = 2.5f;
    private float statusMessageTimer = 0f;
    private String statusMessage = "";

    private final Vector2 tmpWorld = new Vector2();
    private final Rectangle paletteBounds = new Rectangle();
    private final Matrix4 uiMatrix = new Matrix4();

    @Override
    public void create() {
        tileWorldSize = LevelData.TILE_SIZE * TILE_SCALE;

        TileBlueprint[][] blueprint = LevelData.copyBlueprint();
        int rows = blueprint.length;
        int cols = blueprint[0].length;
        worldWidth = cols * tileWorldSize;
        worldHeight = rows * tileWorldSize;

        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(worldWidth, worldHeight, worldCamera);
        worldViewport.apply();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.05f);
        Gdx.graphics.setVSync(true);

        uiMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        loadTileset();
        loadInitialCells(blueprint);
        loadInitialEntities(rows, cols);
        initCollisionGrid(rows, cols);
        if (!paletteRegions.isEmpty()) {
            int defaultIndex = Math.min(atlasIndex(6, 1), paletteRegions.size() - 1);
            setSelectedTileIndex(defaultIndex);
        } else {
            setSelectedTileIndex(-1);
        }
        paletteBridge.updateCurrentSelection(selectedTileIndex);
        spawnPaletteWindowIfPossible();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        elapsed += delta;
        updateStatusBanner(delta);

        drainPaletteSelectionRequests();
        updateHoverState();
        updateEntityHotkeys();
        handleEditorInput();

        ScreenUtils.clear(1f, 1f, 1f, 1f);

        worldCamera.update();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        renderTerrain();
        renderEntitiesOverlay();
        batch.end();

        renderWorldHighlight();
        renderPaletteOverlay();
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, true);
        uiMatrix.setToOrtho2D(0f, 0f, width, height);
        updatePaletteBounds(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        for (Texture texture : paletteTextures) {
            texture.dispose();
        }
    }

    private void loadTileset() {
        paletteTextures.clear();
        paletteRegions.clear();

        TilesetData tsxData = TilesetIO.loadFromTsx(TILESET_TSX);
        if (!tsxData.isEmpty()) {
            paletteTextures.addAll(tsxData.textures());
            paletteRegions.addAll(tsxData.regions());
            paletteCols = tsxData.columns();
            paletteRows = tsxData.rows();
        } else {
            loadPaletteFromTilesDirectory();
            if (paletteRegions.isEmpty()) {
                loadPaletteFromAtlas();
            }
            if (paletteCols <= 0 || paletteRows <= 0) {
                int total = Math.max(1, paletteRegions.size());
                paletteCols = (int) Math.ceil(Math.sqrt(total));
                paletteRows = (int) Math.ceil(total / (float) paletteCols);
            }
        }

        updatePaletteBounds(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
    
    private void loadPaletteFromTilesDirectory() {
        FileHandle directory = Gdx.files.internal(TILESET_BASE);
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Gdx.app.log("Palette", "Tileset folder " + TILESET_BASE + " missing; will fall back to atlas");
            return;
        }
        FileHandle[] contents = directory.list();
        if (contents == null || contents.length == 0) {
            Gdx.app.log("Palette", "No assets found in " + TILESET_BASE + "; will fall back to atlas");
            return;
        }
        Arrays.sort(contents, (a, b) -> a.name().compareToIgnoreCase(b.name()));
        int loaded = 0;
        for (FileHandle file : contents) {
            String extension = file.extension().toLowerCase(Locale.ROOT);
            if ("png".equals(extension)) {
                Texture texture = new Texture(file);
                texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                paletteTextures.add(texture);
                paletteRegions.add(new TextureRegion(texture));
                loaded++;
            } else if ("tsx".equals(extension)) {
                TilesetData data = TilesetIO.loadFromTsx(file);
                if (!data.isEmpty()) {
                    paletteTextures.addAll(data.textures());
                    paletteRegions.addAll(data.regions());
                    paletteCols = data.columns();
                    paletteRows = data.rows();
                    loaded++;
                }
            }
        }
        if (loaded == 0) {
            Gdx.app.log("Palette", "No usable tiles found in " + TILESET_BASE + "; will fall back to atlas");
        } else {
            Gdx.app.log("Palette", "Loaded tiles from " + TILESET_BASE);
        }
    }

    private void loadPaletteFromAtlas() {
        try {
            Texture atlas = new Texture(Gdx.files.internal(TILESET_BASE + "tilemap.png"));
            atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            TextureRegion[][] grid = TextureRegion.split(atlas, LevelData.TILE_SIZE, LevelData.TILE_SIZE);
            paletteCols = grid[0].length;
            paletteRows = grid.length;
            for (TextureRegion[] row : grid) {
                paletteRegions.addAll(Arrays.asList(row));
            }
            paletteTextures.add(atlas);
            Gdx.app.log("Palette", "Fallback to atlas (" + paletteRegions.size() + " tiles)");
        } catch (Exception exception) {
            Gdx.app.error("Palette", "Failed to load fallback tilemap atlas", exception);
        }
    }

    private void spawnPaletteWindowIfPossible() {
        if (paletteWindowSpawned) {
            return;
        }
        if (!(Gdx.app instanceof Lwjgl3Application)) {
            paletteDetached = false;
            return;
        }
        paletteWindowSpawned = true;
        paletteDetached = true;
        Lwjgl3Application application = (Lwjgl3Application) Gdx.app;
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Tileset Palette");
        config.setWindowedMode(560, 960);
        config.setResizable(true);
        config.setForegroundFPS(60);
        application.newWindow(new PaletteWindow(TILESET_TSX, TILESET_BASE, PALETTE_SCALE, paletteBridge), config);
    }

        private void loadInitialCells(TileBlueprint[][] blueprint) {
        int rows = blueprint.length;
        int cols = blueprint[0].length;
        cells = new TileCell[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileBlueprint bp = blueprint[row][col];
                cells[row][col] = TileCell.fromBlueprint(bp);
            }
        }
    }

    private void loadInitialEntities(int rows, int cols) {
        if (entities != null && entities.length == rows && entities[0].length == cols) {
            return;
        }
        entities = new EntityMarker[rows][cols];
    }

    private void initCollisionGrid(int rows, int cols) {
        collision = new CollisionType[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                collision[row][col] = CollisionType.NONE;
            }
        }
    }

    private void renderTerrain() {
        int rows = cells.length;
        int cols = cells[0].length;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileCell cell = cells[row][col];
                TextureRegion region = cell.getFrame(paletteRegions, elapsed);
                if (region == null) {
                    continue;
                }
                float x = col * tileWorldSize;
                float y = (rows - row - 1) * tileWorldSize;
                batch.draw(region, x, y, tileWorldSize, tileWorldSize);
            }
        }
    }

    private void updateHoverState() {
        int screenX = Gdx.input.getX();
        int screenY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (!paletteDetached) {
            pointerOverPalette = paletteBounds.contains(screenX, screenY);
            paletteHoverIndex = pointerOverPalette ? computePaletteIndex(screenX, screenY) : -1;
        } else {
            pointerOverPalette = false;
            paletteHoverIndex = -1;
        }

        if (!paletteDetached && pointerOverPalette) {
            hoverRow = -1;
            hoverCol = -1;
            return;
        }

        tmpWorld.set(Gdx.input.getX(), Gdx.input.getY());
        worldViewport.unproject(tmpWorld);

        int cols = cells[0].length;
        int rows = cells.length;
        int col = (int) Math.floor(tmpWorld.x / tileWorldSize);
        int rowFromBottom = (int) Math.floor(tmpWorld.y / tileWorldSize);
        int row = rows - rowFromBottom - 1;

        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            hoverRow = -1;
            hoverCol = -1;
            return;
        }

        hoverRow = row;
        hoverCol = col;
    }

    private void handleEditorInput() {
        int screenX = Gdx.input.getX();
        int screenY = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        boolean pointerBlocked = !paletteDetached && (pointerOverPalette || draggingPalette);

        if (isSaveShortcutJustPressed()) {
            exportCurrentBlueprint();
        }

        if (!paletteDetached && pointerOverPalette) {
            if (!draggingPalette && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                    && isPointerInPaletteHeader(screenX, screenY)) {
                startPaletteDrag(screenX, screenY);
            } else if (!draggingPalette && paletteHoverIndex >= 0 && Gdx.input.justTouched()) {
                setSelectedTileIndex(paletteHoverIndex);
            }
        }

        if (draggingPalette) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                dragPalette(screenX, screenY);
            } else {
                draggingPalette = false;
            }
            return;
        }

        if (pointerBlocked) {
            return;
        }

        // Collision brush hotkeys: 1=solid, 2=water, 3=door, 4=key, 0=none
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            collisionBrush = CollisionType.SOLID;
            Gdx.app.log("Editor", "Collision brush set to SOLID (1)");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            collisionBrush = CollisionType.WATER;
            Gdx.app.log("Editor", "Collision brush set to WATER (2)");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            collisionBrush = CollisionType.DOOR;
            Gdx.app.log("Editor", "Collision brush set to DOOR (3)");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            collisionBrush = CollisionType.KEY;
            Gdx.app.log("Editor", "Collision brush set to KEY (4)");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0)) {
            collisionBrush = CollisionType.NONE;
            Gdx.app.log("Editor", "Collision brush set to NONE (0)");
        }

        if (shift && hoverRow >= 0 && hoverCol >= 0) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                placeEntity(hoverRow, hoverCol);
            } else if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
                clearEntity(hoverRow, hoverCol);
            }
            return;
        }

        if (hoverRow >= 0 && hoverCol >= 0) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                applyBrush(hoverRow, hoverCol);
            } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                clearCell(hoverRow, hoverCol);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            setSelectedTileIndex(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            adjustFrameCount(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            adjustFrameCount(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            adjustFrameDuration(-FRAME_DURATION_STEP);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)
                || Gdx.input.isKeyJustPressed(Input.Keys.PLUS)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) {
            adjustFrameDuration(FRAME_DURATION_STEP);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            appendFrameToHoveredCell();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            dumpTerrainToConsole();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            clearAllTiles();
        }
    }

    private boolean isSaveShortcutJustPressed() {
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        return ctrl && Gdx.input.isKeyJustPressed(Input.Keys.S);
    }

    private void exportCurrentBlueprint() {
        TileBlueprint[][] snapshot = captureBlueprint();
        boolean saved = LevelData.saveBlueprint(snapshot);
        if (saved) {
            showStatusMessage("Saved level to " + LevelData.blueprintExportPath());
            exportEnemySpawns();
            exportCollisionSnapshot();
        } else {
            showStatusMessage("Save failed (see log)");
        }
    }

    private TileBlueprint[][] captureBlueprint() {
        int rows = cells.length;
        int cols = cells[0].length;
        TileBlueprint[][] snapshot = new TileBlueprint[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileCell cell = cells[row][col];
                if (cell.frameCount <= 0) {
                    snapshot[row][col] = TileBlueprint.air();
                } else {
                    snapshot[row][col] = new TileBlueprint(cell.exportFrames(), cell.frameDuration);
                }
            }
        }
        return snapshot;
    }

    private void updateStatusBanner(float delta) {
        if (statusMessageTimer <= 0f) {
            return;
        }
        statusMessageTimer -= delta;
        if (statusMessageTimer <= 0f) {
            statusMessageTimer = 0f;
            statusMessage = "";
        }
    }

    private void showStatusMessage(String message) {
        statusMessage = message;
        statusMessageTimer = STATUS_MESSAGE_DURATION;
        Gdx.app.log("ViewEditor", message);
    }

    /**
     * Writes collision information (solid, water, door, key) into inspector_snapshot.json
     * so the runtime collision map matches what was painted here.
     */
    private void exportCollisionSnapshot() {
        if (collision == null) {
            return;
        }

        FileHandle handle = Gdx.files.local("inspector_snapshot.json");
        if (handle.file().getParentFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            handle.file().getParentFile().mkdirs();
        }

        try (java.io.Writer writer = handle.writer(false, "UTF-8")) {
            JsonWriter json = new JsonWriter(writer);
            json.setOutputType(JsonWriter.OutputType.json);
            json.object();

            // SOLID tiles
            json.name("solid");
            json.array();
            for (int row = 0; row < collision.length; row++) {
                for (int col = 0; col < collision[row].length; col++) {
                    if (collision[row][col] == CollisionType.SOLID) {
                        json.array();
                        json.value(row);
                        json.value(col);
                        json.pop();
                    }
                }
            }
            json.pop(); // solid array

            // WATER tiles
            json.name("water");
            json.array();
            for (int row = 0; row < collision.length; row++) {
                for (int col = 0; col < collision[row].length; col++) {
                    if (collision[row][col] == CollisionType.WATER) {
                        json.array();
                        json.value(row);
                        json.value(col);
                        json.pop();
                    }
                }
            }
            json.pop(); // water array

            // DOORS / KEYS: simple 1-tile records so existing door system can at least see them
            json.name("doors");
            json.array();
            for (int row = 0; row < collision.length; row++) {
                for (int col = 0; col < collision[row].length; col++) {
                    if (collision[row][col] == CollisionType.DOOR) {
                        json.object();
                        // door tiles
                        json.name("door");
                        json.array();
                        json.array();
                        json.value(row);
                        json.value(col);
                        json.pop();
                        json.pop();
                        // empty key list
                        json.name("key");
                        json.array();
                        json.pop();
                        json.pop();
                    } else if (collision[row][col] == CollisionType.KEY) {
                        json.object();
                        // empty door list
                        json.name("door");
                        json.array();
                        json.pop();
                        // key tiles
                        json.name("key");
                        json.array();
                        json.array();
                        json.value(row);
                        json.value(col);
                        json.pop();
                        json.pop();
                        json.pop();
                    }
                }
            }
            json.pop(); // doors array

            // enemies are now written by enemy_spawns.json, keep empty array for compatibility
            json.name("enemies");
            json.array();
            json.pop();

            json.pop(); // root
            json.close();
            Gdx.app.log("ViewEditor", "Collision snapshot saved to " + handle.file().getAbsolutePath());
        } catch (Exception exception) {
            Gdx.app.error("ViewEditor", "Failed to save inspector_snapshot.json", exception);
        }
    }

    /**
     * Writes all ENEMY entity markers into a standalone JSON file that the game reads
     * for enemy spawns. This lets the map editor drive enemy positions directly.
     *
     * Format (array of objects):
     * [
     *   {"name":"redDeon","row":25,"col":17},
     *   {"name":"deathBoss","row":5,"col":34}
     * ]
     */
    private void exportEnemySpawns() {
        if (entities == null) {
            return;
        }
        FileHandle handle = Gdx.files.local("enemy_spawns.json");
        if (handle.file().getParentFile() != null) {
            //noinspection ResultOfMethodCallIgnored
            handle.file().getParentFile().mkdirs();
        }
        try (java.io.Writer writer = handle.writer(false, "UTF-8")) {
            JsonWriter json = new JsonWriter(writer);
            json.setOutputType(JsonWriter.OutputType.json);
            json.array();

            for (int row = 0; row < entities.length; row++) {
                for (int col = 0; col < entities[row].length; col++) {
                    EntityMarker marker = entities[row][col];
                    if (marker == null || marker.type != EntityType.ENEMY) {
                        continue;
                    }
                    String enemyId = enemyIdForChannel(marker.channel);
                    json.object();
                    json.name("name").value(enemyId);
                    json.name("row").value(marker.row);
                    json.name("col").value(marker.col);
                    json.pop();
                }
            }

            json.pop(); // array
            json.close();
            Gdx.app.log("ViewEditor", "Enemy spawns saved to " + handle.file().getAbsolutePath());
        } catch (Exception exception) {
            Gdx.app.error("ViewEditor", "Failed to save enemy_spawns.json", exception);
        }
    }

    private void drainPaletteSelectionRequests() {
        if (paletteBridge == null) {
            return;
        }
        int requested = paletteBridge.consumeRequestedSelection();
        if (requested != PaletteSharedState.NO_REQUEST) {
            setSelectedTileIndex(requested);
        }
    }

    private void applyBrush(int row, int col) {
        if (selectedTileIndex < 0) {
            clearCell(row, col);
            return;
        }
        int frames = clampFramesForTile(selectedTileIndex, selectedFrameCount);
        cells[row][col].setContiguousFrames(selectedTileIndex, frames, selectedFrameDuration);

        // Also assign collision type for this tile, if a collision brush is active
        if (collision != null && collisionBrush != CollisionType.NONE) {
            collision[row][col] = collisionBrush;
            Gdx.app.log("Editor",
                    "Paint tile #" + selectedTileIndex
                            + " as " + collisionBrush
                            + " at (row=" + row + ", col=" + col + ")");
        } else {
            Gdx.app.log("Editor",
                    "Paint tile #" + selectedTileIndex
                            + " (no collision) at (row=" + row + ", col=" + col + ")");
        }
    }

    private void clearCell(int row, int col) {
        cells[row][col].clear(selectedFrameDuration);
        if (collision != null) {
            collision[row][col] = CollisionType.NONE;
        }
        Gdx.app.log("Editor", "Cleared tile and collision at (row=" + row + ", col=" + col + ")");
    }

    private void setSelectedTileIndex(int tileIndex) {
        if (paletteRegions.isEmpty()) {
            selectedTileIndex = -1;
            selectedFrameCount = 1;
            return;
        }
        if (tileIndex >= paletteRegions.size()) {
            tileIndex = paletteRegions.size() - 1;
        }
        selectedTileIndex = tileIndex;
        selectedFrameCount = clampFramesForTile(tileIndex, selectedFrameCount);
        Gdx.app.log("Palette", tileIndex < 0 ? "Selected: air" : "Selected tile " + describeTile(tileIndex));
        paletteBridge.updateCurrentSelection(selectedTileIndex);
    }

    private void adjustFrameCount(int delta) {
        selectedFrameCount = clampFramesForTile(selectedTileIndex, selectedFrameCount + delta);
    }

    private void adjustFrameDuration(float delta) {
        selectedFrameDuration = Math.max(MIN_FRAME_DURATION,
                Math.min(MAX_FRAME_DURATION, selectedFrameDuration + delta));
    }

    private void dumpTerrainToConsole() {
        StringBuilder sb = new StringBuilder();
        sb.append("public static final TileBlueprint[][] CUSTOM_MAP = {\n");
        for (int row = 0; row < cells.length; row++) {
            sb.append("    {");
            for (int col = 0; col < cells[row].length; col++) {
                TileCell cell = cells[row][col];
                sb.append("new TileBlueprint(new int[]{");
                for (int i = 0; i < cell.frameCount; i++) {
                    sb.append(cell.frameIndices[i]);
                    if (i < cell.frameCount - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("}, ")
                        .append(String.format(Locale.US, "%.2ff", cell.frameDuration))
                        .append(")");
                if (col < cells[row].length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            if (row < cells.length - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n");
        System.out.println(sb);

        if (entities != null) {
            System.out.println("// Entities: type,row,col,channel");
            for (int row = 0; row < entities.length; row++) {
                for (int col = 0; col < entities[row].length; col++) {
                    EntityMarker marker = entities[row][col];
                    if (marker == null) {
                        continue;
                    }
                    String dialog = marker.dialog == null ? "" : (", \"" + marker.dialog + "\"");
                    System.out.println("entity(" + marker.type.name() + ", " + marker.row + ", " + marker.col + ", " + marker.channel + dialog + ");");
                }
            }
        }
    }

    private void clearAllTiles() {
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[row].length; col++) {
                cells[row][col].clear(selectedFrameDuration);
                if (collision != null) {
                    collision[row][col] = CollisionType.NONE;
                }
            }
        }
        Gdx.app.log("Editor", "Cleared map (all air)");
    }

    private void renderWorldHighlight() {
        if (hoverRow < 0 || hoverCol < 0) {
            return;
        }
        int rows = cells.length;
        float x = hoverCol * tileWorldSize;
        float y = (rows - hoverRow - 1) * tileWorldSize;

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
        shapeRenderer.rect(x, y, tileWorldSize, tileWorldSize);
        shapeRenderer.end();
    }

    private void renderPaletteOverlay() {
        if (paletteDetached) {
            return;
        }
        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(paletteBounds.x - 4f, paletteBounds.y - 4f,
                paletteBounds.width + 8f, paletteBounds.height + 8f);
        float headerY = paletteBounds.y + paletteBounds.height - PALETTE_HEADER_HEIGHT;
        shapeRenderer.setColor(0.12f, 0.12f, 0.16f, 0.9f);
        shapeRenderer.rect(paletteBounds.x - 4f, headerY,
                paletteBounds.width + 8f, PALETTE_HEADER_HEIGHT);
        shapeRenderer.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        int total = paletteRegions.size();
        for (int idx = 0; idx < total; idx++) {
            float[] rect = paletteRectForIndex(idx);
            batch.draw(paletteRegions.get(idx), rect[0], rect[1], rect[2], rect[3]);
        }
        
        font.draw(batch, "Tile Palette (drag the top bar)",
                paletteBounds.x, headerY + PALETTE_HEADER_HEIGHT - 8f);
        float infoY = paletteBounds.y + paletteBounds.height + 16f;
        String brushInfo = selectedTileIndex < 0 ? "Brush: air" : "Brush: " + describeTile(selectedTileIndex);
        font.draw(batch, brushInfo, paletteBounds.x, infoY);
        font.draw(batch,
                String.format(Locale.US, "Frames: %d  Duration: %.2fs  ([/], -/+, X=air)",
                        selectedFrameCount, selectedFrameDuration),
                paletteBounds.x, infoY - 16f);
        String entityInfo = "Entity brush: " + entityBrush.displayName;
        if (entityBrush == EntityType.KEY || entityBrush == EntityType.DOOR || entityBrush == EntityType.ENEMY) {
            entityInfo += " (channel " + entityChannel + ")";
        }
        if (entityBrush == EntityType.ENEMY) {
            entityInfo += " -> " + enemyIdForChannel(entityChannel);
        }
        font.draw(batch, entityInfo + "  [F1-F5] brush  channels [F6/F7]  Shift+click to place/remove",
                paletteBounds.x, infoY - 32f);

        String collisionInfo = "Collision brush: " + switch (collisionBrush) {
            case SOLID -> "SOLID (1)";
            case WATER -> "WATER (2)";
            case DOOR -> "DOOR (3)";
            case KEY -> "KEY (4)";
            default -> "NONE (0)";
        };
        font.draw(batch, collisionInfo + "  [1-4] type, 0=none (applies when you paint a tile)",
                paletteBounds.x, infoY - 48f);
        if (statusMessageTimer > 0f && statusMessage != null && !statusMessage.isEmpty()) {
            font.draw(batch, statusMessage, paletteBounds.x, infoY - 64f);
        }
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
        shapeRenderer.rect(paletteBounds.x - 4f, paletteBounds.y - 4f,
                paletteBounds.width + 8f, paletteBounds.height + 8f);
        if (selectedTileIndex >= 0) {
            drawPaletteRect(selectedTileIndex, 0.95f, 0.85f, 0.2f);
        }
        if (paletteHoverIndex >= 0) {
            drawPaletteRect(paletteHoverIndex, 0.9f, 0.4f, 0.4f);
        }
        shapeRenderer.end();
    }

    private void drawPaletteRect(int tileIndex, float r, float g, float b) {
        float[] rect = paletteRectForIndex(tileIndex);
        shapeRenderer.setColor(r, g, b, 1f);
        shapeRenderer.rect(rect[0], rect[1], rect[2], rect[3]);
    }
        
    private int clampFramesForTile(int tileIndex, int desiredFrames) {
        if (tileIndex < 0) {
            return 1;
        }
        int availableFrames = paletteRegions.size() - tileIndex;
        if (availableFrames < 1) {
            availableFrames = 1;
        }
        int maxFrames = Math.min(TileCell.MAX_FRAMES, availableFrames);
        int frames = Math.max(1, desiredFrames);
        return Math.min(frames, maxFrames);
    }

    private void updatePaletteBounds(int width, int height) {
        if (paletteCols <= 0 || paletteRows <= 0) {
            return;
        }
    
        // Maximum area the palette is allowed to use
        float maxWidth = width - 2f * PALETTE_MARGIN;
        float maxHeight = height - 2f * PALETTE_MARGIN - PALETTE_HEADER_HEIGHT;
    
        // Tile size that fits both width and height
        float tileSizeByWidth = maxWidth / Math.max(1, paletteCols);
        float tileSizeByHeight = maxHeight / Math.max(1, paletteRows);
        paletteTileSize = Math.min(tileSizeByWidth, tileSizeByHeight);
    
        // Optional clamp so tiles are not ridiculously tiny
        paletteTileSize = Math.max(8f, paletteTileSize);
    
        float gridWidth = paletteCols * paletteTileSize;
        float gridHeight = paletteRows * paletteTileSize;
        float totalWidth = gridWidth;
        float totalHeight = gridHeight + PALETTE_HEADER_HEIGHT;
    
        if (paletteBounds.width == 0f && paletteBounds.height == 0f) {
            // Initial position: bottom-right corner with margin
            float x = Math.max(0f, width - totalWidth - PALETTE_MARGIN);
            float y = PALETTE_MARGIN;
            paletteBounds.set(x, y, totalWidth, totalHeight);
        } else {
            // Keep it inside the screen if resized
            paletteBounds.width = totalWidth;
            paletteBounds.height = totalHeight;
            float maxX = Math.max(0f, width - totalWidth);
            float maxY = Math.max(0f, height - totalHeight);
            paletteBounds.x = Math.max(0f, Math.min(paletteBounds.x, maxX));
            paletteBounds.y = Math.max(0f, Math.min(paletteBounds.y, maxY));
        }
    }
    
    private String describeTile(int tileIndex) {
        int row = tileIndex / paletteCols;
        int col = tileIndex % paletteCols;
        return String.format(Locale.US, "row %d col %d (#%d)", row, col, tileIndex);
    }

    private void appendFrameToHoveredCell() {
        if (hoverRow < 0 || hoverCol < 0 || selectedTileIndex < 0 || paletteRegions.isEmpty()) {
            return;
        }
        TileCell cell = cells[hoverRow][hoverCol];
        if (cell.addFrame(selectedTileIndex, selectedFrameDuration)) {
            Gdx.app.log("Editor", "Added frame " + describeTile(selectedTileIndex) + " to cell");
        } else {
            Gdx.app.log("Editor", "Cell already has max frames (" + TileCell.MAX_FRAMES + ")");
        }
    }

    private int atlasIndex(int row, int col) {
        return row * paletteCols + col;
    }

    private boolean isPointerInPaletteHeader(int screenX, int screenY) {
        if (!paletteBounds.contains(screenX, screenY)) {
            return false;
        }
        float localY = screenY - paletteBounds.y;
        return localY >= paletteBounds.height - PALETTE_HEADER_HEIGHT;
    }

    private void startPaletteDrag(int screenX, int screenY) {
        draggingPalette = true;
        dragOffsetX = screenX - paletteBounds.x;
        float topY = paletteBounds.y + paletteBounds.height;
        dragOffsetY = topY - screenY;
    }

    private void dragPalette(int screenX, int screenY) {
        float newX = screenX - dragOffsetX;
        float newTop = screenY + dragOffsetY;
        float newY = newTop - paletteBounds.height;
        float maxX = Math.max(0f, Gdx.graphics.getWidth() - paletteBounds.width);
        float maxY = Math.max(0f, Gdx.graphics.getHeight() - paletteBounds.height);
        paletteBounds.x = Math.max(0f, Math.min(newX, maxX));
        paletteBounds.y = Math.max(0f, Math.min(newY, maxY));
    }

    private int computePaletteIndex(float screenX, float screenY) {
        if (!paletteBounds.contains(screenX, screenY)) {
            return -1;
        }
        float localX = screenX - paletteBounds.x;
        float localY = screenY - paletteBounds.y;
        float usableHeight = paletteRows * paletteTileSize;
        if (localY >= usableHeight) {
            return -1; // in the header area
        }
        int col = (int) (localX / paletteTileSize);
        int rowFromBottom = (int) (localY / paletteTileSize);
        int row = paletteRows - rowFromBottom - 1;
        if (col < 0 || col >= paletteCols || row < 0 || row >= paletteRows) {
            return -1;
        }
        return row * paletteCols + col;
    }
        private float[] paletteRectForIndex(int tileIndex) {
        int row = tileIndex / paletteCols;
        int col = tileIndex % paletteCols;
        float x = paletteBounds.x + col * paletteTileSize;
        float y = paletteBounds.y + (paletteRows - row - 1) * paletteTileSize;
        return new float[]{x, y, paletteTileSize, paletteTileSize};
    }
    
    private void updateEntityHotkeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            entityBrush = EntityType.NONE;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            entityBrush = EntityType.ENEMY;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            entityBrush = EntityType.GRANDPA;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            entityBrush = EntityType.KEY;
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            entityBrush = EntityType.DOOR;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            entityChannel = Math.max(0, entityChannel - 1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            entityChannel = Math.min(999, entityChannel + 1);
        }
    }

    private void renderEntitiesOverlay() {
        if (entities == null) {
            return;
        }
        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int row = 0; row < entities.length; row++) {
            for (int col = 0; col < entities[row].length; col++) {
                EntityMarker marker = entities[row][col];
                if (marker == null) {
                    continue;
                }
                float x = marker.col * tileWorldSize;
                float y = (entities.length - marker.row - 1) * tileWorldSize;
                float padding = 4f;
                shapeRenderer.setColor(marker.type.r, marker.type.g, marker.type.b, 0.95f);
                shapeRenderer.rect(x + padding, y + padding, tileWorldSize - padding * 2f, tileWorldSize - padding * 2f);
            }
        }
        shapeRenderer.end();
    }

    private void placeEntity(int row, int col) {
        if (entityBrush == EntityType.NONE || entities == null) {
            return;
        }
        entities[row][col] = new EntityMarker(row, col, entityBrush, entityChannel);
        if (entityBrush == EntityType.GRANDPA) {
            Gdx.app.log("Entities", "Placed grandpa NPC at (" + row + "," + col + ")");
        } else if (entityBrush == EntityType.KEY || entityBrush == EntityType.DOOR) {
            Gdx.app.log("Entities", "Placed " + entityBrush.displayName + " channel " + entityChannel + " at (" + row + "," + col + ")");
        } else {
            Gdx.app.log("Entities", "Placed " + entityBrush.displayName + " at (" + row + "," + col + ")");
        }
    }

    private void clearEntity(int row, int col) {
        if (entities == null) {
            return;
        }
        if (entities[row][col] != null) {
            Gdx.app.log("Entities", "Removed " + entities[row][col].type.displayName + " from (" + row + "," + col + ")");
        }
        entities[row][col] = null;
    }

    private static String enemyIdForChannel(int channel) {
        return switch (channel) {
            case 1 -> "deathBoss";
            case 2 -> "skeletonEnemie";
            default -> "redDeon";
        };
    }

    private static class TileCell {
        static final int MAX_FRAMES = 3;

        final int[] frameIndices = new int[MAX_FRAMES];
        int frameCount;
        float frameDuration = 0.15f;

        static TileCell fromBlueprint(TileBlueprint blueprint) {
            TileCell cell = new TileCell();
            cell.setFrames(blueprint.frames(), blueprint.frameDuration());
            return cell;
        }

        void setContiguousFrames(int startIndex, int frames, float duration) {
            frameCount = Math.min(frames, MAX_FRAMES);
            for (int i = 0; i < frameCount; i++) {
                frameIndices[i] = startIndex + i;
            }
            frameDuration = duration;
        }

        void setFrames(int[] frames, float duration) {
            frameCount = Math.min(frames.length, MAX_FRAMES);
            for (int i = 0; i < frameCount; i++) {
                frameIndices[i] = frames[i];
            }
            frameDuration = duration;
        }

        void clear(float duration) {
            frameCount = 0;
            frameDuration = duration;
        }

        boolean addFrame(int tileIndex, float duration) {
            if (frameCount >= MAX_FRAMES) {
                return false;
            }
            if (frameCount < 0 || frameCount >= MAX_FRAMES) {
                Gdx.app.error("TileCell", "Invalid frameCount " + frameCount + " before adding frame");
            }
            frameIndices[frameCount++] = tileIndex;
            frameDuration = duration;
            return true;
        }

        TextureRegion getFrame(List<TextureRegion> palette, float time) {
            if (frameCount <= 0) {
                return null;
            }
            float duration = Math.max(frameDuration, 0.01f);
            int frame = (int) Math.floor((time / duration) % frameCount);
            int index = frameIndices[frame];
            if (index < 0 || index >= palette.size()) {
                return null;
            }
            return palette.get(index);
        }

        int[] exportFrames() {
            if (frameCount <= 0) {
                return new int[0];
            }
            int[] frames = new int[frameCount];
            System.arraycopy(frameIndices, 0, frames, 0, frameCount);
            return frames;
        }
    }

    private enum EntityType {
        NONE("None", 0f, 0f, 0f),
        ENEMY("Enemy", 0.95f, 0.2f, 0.2f),
        GRANDPA("Grandpa", 0.25f, 0.85f, 1f),
        KEY("Key", 0.95f, 0.8f, 0.2f),
        DOOR("Door", 0.95f, 0.55f, 0.15f);

        final String displayName;
        final float r;
        final float g;
        final float b;

        EntityType(String displayName, float r, float g, float b) {
            this.displayName = displayName;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static class EntityMarker {
        final int row;
        final int col;
        final EntityType type;
        final int channel;
        final String dialog;

        EntityMarker(int row, int col, EntityType type, int channel) {
            this.row = row;
            this.col = col;
            this.type = type;
            this.channel = channel;
            this.dialog = type == EntityType.GRANDPA ? "I'm proud of you, kid." : null;
        }
    }
}

