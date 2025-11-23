package org.celestelike.game.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.celestelike.game.world.LevelData.TileBlueprint;

/**
 * Lightweight collision layer derived from the authored tile blueprint (and optional inspector
 * snapshot) so gameplay systems can treat tagged tiles as solid ground or walls.
 */
public final class LevelCollisionMap {

    private static final String SNAPSHOT_PATH = "inspector_snapshot.json";
    private static final float DEFAULT_TILE_SIZE = org.celestelike.game.world.LevelData.TILE_SIZE;

    private final boolean[][] solid;
    private final boolean[][] water;
    private final int rows;
    private final int cols;
    private final float tileSize;
    private boolean solidMaskLoaded;
    private boolean waterMaskLoaded;

    public LevelCollisionMap(TileBlueprint[][] blueprint, float tileWorldSize) {
        this.rows = blueprint.length;
        this.cols = blueprint[0].length;
        this.tileSize = tileWorldSize <= 0f ? DEFAULT_TILE_SIZE : tileWorldSize;
        this.solid = new boolean[rows][cols];
        this.water = new boolean[rows][cols];

        solidMaskLoaded = false;
        waterMaskLoaded = false;

        // SOLIDS NOW COME *ONLY* FROM THE INSPECTOR SNAPSHOT.
        // We no longer auto-generate solid tiles from the art/blueprint,
        // so there is no way to get a solid block unless the snapshot says so.
        boolean loadedSnapshot = loadMasksFromSnapshot();

        if (!waterMaskLoaded) {
            populateWaterMask(blueprint);
        }
        if (loadedSnapshot) {
            logInfo("Collision map loaded from snapshot " + SNAPSHOT_PATH);
        }
    }

    public float tileSize() {
        return tileSize;
    }

    public boolean isSolid(int row, int col) {
        if (col < 0 || col >= cols) {
            return true;
        }
        if (row < 0) {
            return true;
        }
        if (row >= rows) {
            return true;
        }
        return solid[row][col];
    }

    public boolean isWater(int row, int col) {
        if (col < 0 || col >= cols) {
            return false;
        }
        if (row < 0 || row >= rows) {
            return false;
        }
        return water[row][col];
    }

    public boolean overlapsWater(float left, float bottom, float right, float top) {
        if (left > right || bottom > top) {
            return false;
        }
        int colStart = worldToCol(Math.max(left, 0f));
        int colEnd = worldToCol(Math.max(right, 0f));
        int rowStart = worldToRow(Math.max(bottom, 0f));
        int rowEnd = worldToRow(Math.max(top, 0f));
        for (int row = Math.min(rowStart, rowEnd); row <= Math.max(rowStart, rowEnd); row++) {
            for (int col = Math.min(colStart, colEnd); col <= Math.max(colStart, colEnd); col++) {
                if (isWater(row, col)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int worldToCol(float worldX) {
        return (int) Math.floor(worldX / tileSize);
    }

    public int worldToRow(float worldY) {
        int rowFromBottom = (int) Math.floor(worldY / tileSize);
        return rows - rowFromBottom - 1;
    }

    public float colLeft(int col) {
        return col * tileSize;
    }

    public float rowBottom(int row) {
        return (rows - row - 1) * tileSize;
    }

    public float rowTop(int row) {
        return rowBottom(row) + tileSize;
    }

    private void populateFromBlueprint(TileBlueprint[][] blueprint) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileBlueprint cell = blueprint[row][col];
                solid[row][col] = cell != null && cell.frames().length > 0;
            }
        }
        logInfo("Collision map populated from blueprint (no snapshot found)");
    }

    private void populateWaterMask(TileBlueprint[][] blueprint) {
        int waterTileIndex = org.celestelike.game.world.LevelData.waterTileIndex();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileBlueprint cell = blueprint[row][col];
                water[row][col] = containsFrame(cell, waterTileIndex);
            }
        }
    }

    private static boolean containsFrame(TileBlueprint cell, int frameIndex) {
        if (cell == null || cell.frames() == null) {
            return false;
        }
        for (int frame : cell.frames()) {
            if (frame == frameIndex) {
                return true;
            }
        }
        return false;
    }

    private boolean loadMasksFromSnapshot() {
        try {
            FileHandle file = locateSnapshot();
            if (file == null) {
                return false;
            }
            JsonValue root = new JsonReader().parse(file);
            boolean loadedAnything = false;
            JsonValue solidArray = root.get("solid");
            if (solidArray != null && solidArray.size > 0) {
                applySolidEntries(solidArray);
                solidMaskLoaded = true;
                loadedAnything = true;
            }
            JsonValue waterArray = root.get("water");
            if (waterArray != null && waterArray.size > 0) {
                applyWaterEntries(waterArray);
                waterMaskLoaded = true;
                loadedAnything = true;
            }
            return loadedAnything;
        } catch (Exception exception) {
            logError("Failed to parse inspector snapshot for collisions", exception);
            return false;
        }
    }

    private FileHandle locateSnapshot() {
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

    private void applySolidEntries(JsonValue array) {
        JsonValue entry = array.child;
        while (entry != null) {
            if (entry.isArray()) {
                if (entry.size >= 2) {
                    markSolid(entry.getInt(0), entry.getInt(1));
                }
                entry = entry.next;
            } else if (entry.isObject()) {
                JsonValue rowValue = entry.get("row");
                JsonValue colValue = entry.get("col");
                if (rowValue != null && colValue != null) {
                    markSolid(rowValue.asInt(), colValue.asInt());
                }
                entry = entry.next;
            } else if (entry.isNumber()) {
                JsonValue colValue = entry.next;
                if (colValue == null || !colValue.isNumber()) {
                    break;
                }
                markSolid(entry.asInt(), colValue.asInt());
                entry = colValue.next;
            } else {
                entry = entry.next;
            }
        }
    }

    private void applyWaterEntries(JsonValue array) {
        JsonValue entry = array.child;
        while (entry != null) {
            if (entry.isArray()) {
                if (entry.size >= 2) {
                    markWater(entry.getInt(0), entry.getInt(1));
                }
                entry = entry.next;
            } else if (entry.isObject()) {
                JsonValue rowValue = entry.get("row");
                JsonValue colValue = entry.get("col");
                if (rowValue != null && colValue != null) {
                    markWater(rowValue.asInt(), colValue.asInt());
                }
                entry = entry.next;
            } else if (entry.isNumber()) {
                JsonValue colValue = entry.next;
                if (colValue == null || !colValue.isNumber()) {
                    break;
                }
                markWater(entry.asInt(), colValue.asInt());
                entry = colValue.next;
            } else {
                entry = entry.next;
            }
        }
    }

    private void markSolid(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }
        solid[row][col] = true;
    }

    private void markWater(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }
        water[row][col] = true;
    }

    private static void logInfo(String message) {
        if (Gdx.app != null) {
            Gdx.app.log("LevelCollisionMap", message);
        }
    }

    private static void logError(String message, Exception exception) {
        if (Gdx.app != null) {
            Gdx.app.error("LevelCollisionMap", message, exception);
        } else if (exception != null) {
            exception.printStackTrace();
        }
    }
}

