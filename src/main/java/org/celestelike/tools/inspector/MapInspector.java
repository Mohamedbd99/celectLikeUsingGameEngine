package org.celestelike.tools.inspector;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.celestelike.game.world.LevelData;
import org.celestelike.game.world.LevelData.TileBlueprint;

public class MapInspector extends ApplicationAdapter {

    private enum SelectionType {
        SOLID("Solid ground/wall"),
        ENEMY_TIER1("Enemies tier 1"),
        ENEMY_TIER2("Enemies tier 2"),
        ENEMY_TIER3("Enemies tier 3"),
        DOOR("Door"),
        KEY("Door Key"),
        WATER("Water");

        final String label;

        SelectionType(String label) {
            this.label = label;
        }
    }

    private record TileRef(int row, int col) {
        @Override public String toString() {
            return "(r=%d,c=%d)".formatted(row, col);
        }
    }

    private static class DoorRecord {
        final int id;
        final List<TileRef> doorTiles = new ArrayList<>();
        final List<TileRef> keyTiles = new ArrayList<>();

        DoorRecord(int id) {
            this.id = id;
        }

        boolean hasKey() {
            return !keyTiles.isEmpty();
        }

        @Override public String toString() {
            return "Door#" + id + " door=" + doorTiles + " key=" + keyTiles;
        }
    }

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    private static final String SNAPSHOT_PATH = "inspector_snapshot.json";

    private final List<TextureRegion> paletteRegions = new ArrayList<>();
    private final List<Texture> paletteTextures = new ArrayList<>();

    private TileCell[][] cells;
    private float tileWorldSize;
    private float worldWidth;
    private float worldHeight;
    private float elapsed;

    private SelectionType activeSelection = SelectionType.SOLID;
    private final Map<SelectionType, List<TileRef>> selections = new EnumMap<>(SelectionType.class);
    private final List<DoorRecord> doors = new ArrayList<>();
    private final ArrayDeque<DoorRecord> pendingDoors = new ArrayDeque<>();
    private int enemyTierIndex = 0;
    private boolean deleteMode = false;

    @Override
    public void create() {
        tileWorldSize = LevelData.TILE_SIZE * 4f;

        TileBlueprint[][] blueprint = LevelData.copyBlueprint();
        int rows = blueprint.length;
        int cols = blueprint[0].length;
        cells = new TileCell[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cells[row][col] = TileCell.fromBlueprint(blueprint[row][col]);
            }
        }

        worldWidth = cols * tileWorldSize;
        worldHeight = rows * tileWorldSize;

        camera = new OrthographicCamera();
        viewport = new FitViewport(worldWidth, worldHeight, camera);
        viewport.apply(true);
        camera.position.set(worldWidth / 2f, worldHeight / 2f, 0f);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        loadTileset();
        Gdx.graphics.setVSync(true);

        for (SelectionType type : SelectionType.values()) {
            selections.put(type, new ArrayList<>());
        }
        loadSnapshot();
        printSummary();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        elapsed += delta;

        handleHotkeys();
        handleClicks();

        ScreenUtils.clear(0.07f, 0.07f, 0.1f, 1f);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawTiles();
        batch.end();

        drawHighlights();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        for (Texture texture : paletteTextures) {
            texture.dispose();
        }
    }

    private void drawTiles() {
        int rows = cells.length;
        int cols = cells[0].length;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TextureRegion frame = cells[row][col].getFrame(paletteRegions, elapsed);
                if (frame == null) {
                    continue;
                }
                float x = col * tileWorldSize;
                float y = (rows - row - 1) * tileWorldSize;
                batch.draw(frame, x, y, tileWorldSize, tileWorldSize);
            }
        }
    }

    private void drawHighlights() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        selections.forEach((type, list) -> {
            setFillColor(type, 0.28f);
            list.forEach(ref -> drawTileFill(ref));
        });
        for (DoorRecord door : doors) {
            shapeRenderer.setColor(0.95f, 0.55f, 0.15f, 0.28f);
            door.doorTiles.forEach(this::drawTileFill);
            shapeRenderer.setColor(0.95f, 0.85f, 0.2f, 0.28f);
            door.keyTiles.forEach(this::drawTileFill);
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        selections.forEach((type, list) -> {
            setOutlineColor(type);
            list.forEach(ref -> drawTileOutline(ref));
        });
        shapeRenderer.setColor(0.95f, 0.55f, 0.15f, 1f);
        doors.forEach(door -> door.doorTiles.forEach(this::drawTileOutline));
        shapeRenderer.setColor(0.95f, 0.85f, 0.2f, 1f);
        doors.forEach(door -> door.keyTiles.forEach(this::drawTileOutline));
        shapeRenderer.end();
    }

    private void drawTileOutline(TileRef ref) {
        int rows = cells.length;
        float x = ref.col * tileWorldSize;
        float y = (rows - ref.row - 1) * tileWorldSize;
        shapeRenderer.rect(x, y, tileWorldSize, tileWorldSize);
    }

    private void drawTileFill(TileRef ref) {
        int rows = cells.length;
        float x = ref.col * tileWorldSize;
        float y = (rows - ref.row - 1) * tileWorldSize;
        shapeRenderer.rect(x, y, tileWorldSize, tileWorldSize);
    }

    private void setFillColor(SelectionType type, float alpha) {
        switch (type) {
            case SOLID -> shapeRenderer.setColor(0.1f, 0.95f, 0.2f, alpha);
            case WATER -> shapeRenderer.setColor(0.2f, 0.45f, 0.95f, alpha);
            case ENEMY_TIER1 -> shapeRenderer.setColor(0.95f, 0.2f, 0.2f, alpha);
            case ENEMY_TIER2 -> shapeRenderer.setColor(0.95f, 0.6f, 0.2f, alpha);
            case ENEMY_TIER3 -> shapeRenderer.setColor(0.7f, 0.2f, 0.95f, alpha);
            case DOOR -> shapeRenderer.setColor(0.95f, 0.55f, 0.15f, alpha);
            case KEY -> shapeRenderer.setColor(0.95f, 0.85f, 0.2f, alpha);
        }
    }

    private void setOutlineColor(SelectionType type) {
        switch (type) {
            case SOLID -> shapeRenderer.setColor(0.1f, 0.95f, 0.2f, 1f);
            case WATER -> shapeRenderer.setColor(0.2f, 0.45f, 0.95f, 1f);
            case ENEMY_TIER1 -> shapeRenderer.setColor(0.95f, 0.2f, 0.2f, 1f);
            case ENEMY_TIER2 -> shapeRenderer.setColor(0.95f, 0.6f, 0.2f, 1f);
            case ENEMY_TIER3 -> shapeRenderer.setColor(0.7f, 0.2f, 0.95f, 1f);
            case DOOR -> shapeRenderer.setColor(0.95f, 0.55f, 0.15f, 1f);
            case KEY -> shapeRenderer.setColor(0.95f, 0.85f, 0.2f, 1f);
        }
    }

    private void handleHotkeys() {
        if (isSaveShortcutJustPressed()) {
            saveSnapshot();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            setSelection(SelectionType.SOLID, "1");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            setSelection(SelectionType.SOLID, "2");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            cycleEnemyTier();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            setSelection(SelectionType.DOOR, "4");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            setSelection(SelectionType.KEY, "5");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            setSelection(SelectionType.WATER, "6");
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.DEL)
                || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            deleteMode = true;
            Gdx.app.log("MapInspector", "Delete mode active. Click a tile to remove it.");
        }
    }

    private boolean isSaveShortcutJustPressed() {
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        return ctrl && Gdx.input.isKeyJustPressed(Input.Keys.S);
    }

    private void cycleEnemyTier() {
        enemyTierIndex = (enemyTierIndex + 1) % 3;
        SelectionType type = switch (enemyTierIndex) {
            case 0 -> SelectionType.ENEMY_TIER1;
            case 1 -> SelectionType.ENEMY_TIER2;
            default -> SelectionType.ENEMY_TIER3;
        };
        setSelection(type, "3 (tier " + (enemyTierIndex + 1) + ")");
    }

    private void setSelection(SelectionType type, String keyLabel) {
        activeSelection = type;
        Gdx.app.log("MapInspector", "Active selection: " + type.label + " (key " + keyLabel + ")");
    }

    private void handleClicks() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }
        Vector2 world = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        TileRef ref = worldToTile(world.x, world.y);
        if (ref == null) {
            return;
        }
        if (deleteMode) {
            deleteMode = false;
            removeTile(ref);
            return;
        }
        switch (activeSelection) {
            case SOLID -> addAndLog(SelectionType.SOLID, ref);
            case ENEMY_TIER1 -> addAndLog(SelectionType.ENEMY_TIER1, ref);
            case ENEMY_TIER2 -> addAndLog(SelectionType.ENEMY_TIER2, ref);
            case ENEMY_TIER3 -> addAndLog(SelectionType.ENEMY_TIER3, ref);
            case WATER -> addAndLog(SelectionType.WATER, ref);
            case DOOR -> handleDoorSelection(ref);
            case KEY -> handleKeySelection(ref);
        }
    }

    private void addAndLog(SelectionType type, TileRef ref) {
        List<TileRef> list = selections.get(type);
        if (list.contains(ref)) {
            Gdx.app.log("MapInspector", type.label + " already contains " + ref);
            return;
        }
        list.add(ref);
        printSummary();
    }

    private void handleDoorSelection(TileRef ref) {
        DoorRecord current = pendingDoors.peekLast();
        if (current == null || current.hasKey()) {
            current = new DoorRecord(doors.size() + 1);
            doors.add(current);
            pendingDoors.addLast(current);
        }
        if (current.doorTiles.contains(ref)) {
            Gdx.app.log("MapInspector", "Door already contains " + ref);
            return;
        }
        current.doorTiles.add(ref);
        printSummary();
    }

    private void handleKeySelection(TileRef ref) {
        DoorRecord current = pendingDoors.peekLast();
        if (current == null) {
            Gdx.app.log("MapInspector", "No pending door to attach this key. Select door (4) first.");
            return;
        }
        if (current.keyTiles.contains(ref)) {
            Gdx.app.log("MapInspector", "Key list already contains " + ref);
            return;
        }
        current.keyTiles.add(ref);
        if (current.hasKey()) {
            pendingDoors.remove(current);
        }
        printSummary();
    }

    private void removeTile(TileRef ref) {
        boolean removed = false;
        for (SelectionType type : SelectionType.values()) {
            List<TileRef> list = selections.get(type);
            if (list.removeIf(existing -> existing.equals(ref))) {
                removed = true;
            }
        }
        for (int i = doors.size() - 1; i >= 0; i--) {
            DoorRecord door = doors.get(i);
            boolean changed = door.doorTiles.removeIf(existing -> existing.equals(ref));
            changed |= door.keyTiles.removeIf(existing -> existing.equals(ref));
            if (door.doorTiles.isEmpty() && door.keyTiles.isEmpty()) {
                doors.remove(i);
                changed = true;
            }
            if (changed) {
                removed = true;
            }
        }
        if (removed) {
            printSummary();
        } else {
            Gdx.app.log("MapInspector", "No entry found at " + ref);
        }
    }

    private TileRef worldToTile(float worldX, float worldY) {
        int col = (int) Math.floor(worldX / tileWorldSize);
        int rowFromBottom = (int) Math.floor(worldY / tileWorldSize);
        int row = cells.length - rowFromBottom - 1;
        if (col < 0 || col >= cells[0].length || row < 0 || row >= cells.length) {
            return null;
        }
        return new TileRef(row, col);
    }

    private void printSummary() {
        logCategory("solid", selections.get(SelectionType.SOLID));
        logCategory("water", selections.get(SelectionType.WATER));
        logCategory("enemires tier 1", selections.get(SelectionType.ENEMY_TIER1));
        logCategory("enemires tier 2", selections.get(SelectionType.ENEMY_TIER2));
        logCategory("enemires tier 3", selections.get(SelectionType.ENEMY_TIER3));
        logDoors();
    }

    private void logCategory(String label, List<TileRef> refs) {
        StringBuilder sb = new StringBuilder();
        sb.append(label).append(" [");
        for (int i = 0; i < refs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("#").append(i + 1).append(" ").append(refs.get(i));
        }
        sb.append("]");
        Gdx.app.log("MapInspector", sb.toString());
    }

    private void logDoors() {
        if (doors.isEmpty()) {
            Gdx.app.log("MapInspector", "door []");
            return;
        }
        StringBuilder sb = new StringBuilder("door [");
        for (int i = 0; i < doors.size(); i++) {
            DoorRecord door = doors.get(i);
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("#").append(i + 1)
                    .append("{door=").append(formatRefs(door.doorTiles))
                    .append(", key=").append(formatRefs(door.keyTiles)).append("}");
        }
        sb.append("]");
        Gdx.app.log("MapInspector", sb.toString());
    }

    private String formatRefs(List<TileRef> refs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < refs.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("#").append(i + 1).append(" ").append(refs.get(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private void loadTileset() {
        loadPaletteFromTilesDirectory();
        if (paletteRegions.isEmpty()) {
            loadPaletteFromAtlas();
        }
        if (paletteRegions.isEmpty()) {
            throw new IllegalStateException("Unable to load tile palette.");
        }
    }

    private void loadPaletteFromTilesDirectory() {
        String basePath = "assets/kenney_pico-8-platformer/Transparent/Tiles/";
        boolean any = false;
        int consecutiveMisses = 0;
        for (int index = 0; index < 512 && consecutiveMisses < 50; index++) {
            String name = String.format(Locale.US, "tile_%04d.png", index);
            FileHandle file = Gdx.files.internal(basePath + name);
            if (!file.exists()) {
                consecutiveMisses++;
                continue;
            }
            Texture texture = new Texture(file);
            paletteTextures.add(texture);
            paletteRegions.add(new TextureRegion(texture));
            consecutiveMisses = 0;
            any = true;
        }
        if (any) {
            Gdx.app.log("MapInspector", "Loaded " + paletteRegions.size() + " sprites from Transparent/Tiles");
        }
    }

    private void loadPaletteFromAtlas() {
        FileHandle file = Gdx.files.internal("assets/kenney_pico-8-platformer/Transparent/Tilemap/tilemap.png");
        if (!file.exists()) {
            return;
        }
        Texture atlas = new Texture(file);
        paletteTextures.add(atlas);
        int tileSize = LevelData.TILE_SIZE;
        int rows = atlas.getHeight() / tileSize;
        int cols = atlas.getWidth() / tileSize;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TextureRegion region = new TextureRegion(atlas,
                        col * tileSize,
                        row * tileSize,
                        tileSize,
                        tileSize);
                paletteRegions.add(region);
            }
        }
    }

    private void loadSnapshot() {
        FileHandle file = snapshotHandleForRead();
        if (file == null) {
            Gdx.app.log("MapInspector", "No snapshot found; starting fresh.");
            return;
        }
        try {
            selections.values().forEach(List::clear);
            doors.clear();
            pendingDoors.clear();
            JsonValue root = new JsonReader().parse(file);
            preloadList(SelectionType.SOLID, root.get("solid"));
            preloadList(SelectionType.WATER, root.get("water"));
            preloadList(SelectionType.ENEMY_TIER1, root.get("enemyTier1"));
            preloadList(SelectionType.ENEMY_TIER2, root.get("enemyTier2"));
            preloadList(SelectionType.ENEMY_TIER3, root.get("enemyTier3"));
            JsonValue doorArray = root.get("doors");
            if (doorArray != null) {
                for (JsonValue doorValue : doorArray) {
                    DoorRecord door = new DoorRecord(doors.size() + 1);
                    applyRefs(doorValue.get("door"), door.doorTiles);
                    applyRefs(doorValue.get("key"), door.keyTiles);
                    doors.add(door);
                }
            }
            Gdx.app.log("MapInspector", "Loaded snapshot from " + file.path());
        } catch (Exception exception) {
            Gdx.app.error("MapInspector", "Failed to load snapshot", exception);
        }
    }

    private void preloadList(SelectionType type, JsonValue value) {
        if (value == null) {
            return;
        }
        applyRefs(value, selections.get(type));
    }

    private void applyRefs(JsonValue array, List<TileRef> target) {
        if (array == null) {
            return;
        }
        JsonValue entry = array.child;
        while (entry != null) {
            if (entry.isArray()) {
                if (entry.size >= 2) {
                    target.add(new TileRef(entry.getInt(0), entry.getInt(1)));
                }
                entry = entry.next;
            } else if (entry.isNumber()) {
                JsonValue colValue = entry.next;
                if (colValue == null || !colValue.isNumber()) {
                    break;
                }
                target.add(new TileRef(entry.asInt(), colValue.asInt()));
                entry = colValue.next;
            } else {
                entry = entry.next;
            }
        }
    }

    private void saveSnapshot() {
        FileHandle handle = snapshotHandleForWrite();
        if (handle == null) {
            Gdx.app.error("MapInspector", "File IO unavailable; cannot save snapshot.");
            return;
        }
        if (handle.file().getParentFile() != null) {
            handle.file().getParentFile().mkdirs();
        }
        try (Writer writer = handle.writer(false, "UTF-8")) {
            JsonWriter json = new JsonWriter(writer);
            json.setOutputType(JsonWriter.OutputType.json);
            json.object();
            writeRefs(json, "solid", selections.get(SelectionType.SOLID));
            writeRefs(json, "water", selections.get(SelectionType.WATER));
            writeRefs(json, "enemyTier1", selections.get(SelectionType.ENEMY_TIER1));
            writeRefs(json, "enemyTier2", selections.get(SelectionType.ENEMY_TIER2));
            writeRefs(json, "enemyTier3", selections.get(SelectionType.ENEMY_TIER3));
            json.name("doors");
            json.array();
            for (DoorRecord door : doors) {
                json.object();
                writeRefs(json, "door", door.doorTiles);
                writeRefs(json, "key", door.keyTiles);
                json.pop();
            }
            json.pop(); // doors array
            json.pop(); // root
            json.close();
            Gdx.app.log("MapInspector", "Snapshot saved to " + handle.file().getAbsolutePath());
            printSummary();
        } catch (IOException exception) {
            Gdx.app.error("MapInspector", "Failed to save snapshot", exception);
        }
    }

    private void writeRefs(JsonWriter json, String name, List<TileRef> refs) throws IOException {
        json.name(name);
        json.array();
        for (TileRef ref : refs) {
            json.array();
            json.value(ref.row());
            json.value(ref.col());
            json.pop();
        }
        json.pop();
    }

    private FileHandle snapshotHandleForRead() {
        if (Gdx.files == null) {
            return null;
        }
        FileHandle local = Gdx.files.local(SNAPSHOT_PATH);
        if (local.exists()) {
            return local;
        }
        FileHandle internal = Gdx.files.internal(SNAPSHOT_PATH);
        if (internal.exists()) {
            return internal;
        }
        return null;
    }

    private FileHandle snapshotHandleForWrite() {
        if (Gdx.files == null) {
            return null;
        }
        return Gdx.files.local(SNAPSHOT_PATH);
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

        void setFrames(int[] frames, float duration) {
            frameCount = Math.min(frames.length, MAX_FRAMES);
            for (int i = 0; i < frameCount; i++) {
                frameIndices[i] = frames[i];
            }
            frameDuration = duration;
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
    }
}

