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
    private static final float DEFAULT_TILE_SIZE = LevelData.TILE_SIZE;

    private final boolean[][] solid;
    private final int rows;
    private final int cols;
    private final float tileSize;

    public LevelCollisionMap(TileBlueprint[][] blueprint, float tileWorldSize) {
        this.rows = blueprint.length;
        this.cols = blueprint[0].length;
        this.tileSize = tileWorldSize <= 0f ? DEFAULT_TILE_SIZE : tileWorldSize;
        this.solid = new boolean[rows][cols];

        boolean loadedSnapshot = loadSolidMaskFromSnapshot();
        if (!loadedSnapshot) {
            populateFromBlueprint(blueprint);
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

    private boolean loadSolidMaskFromSnapshot() {
        try {
            FileHandle file = locateSnapshot();
            if (file == null) {
                return false;
            }
            JsonValue root = new JsonReader().parse(file);
            JsonValue solidArray = root.get("solid");
            if (solidArray == null || solidArray.size == 0) {
                return false;
            }
            applySolidEntries(solidArray);
            logInfo("Collision map loaded from snapshot " + file.path());
            return true;
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

    private void markSolid(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return;
        }
        solid[row][col] = true;
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

