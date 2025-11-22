package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads enemy spawn coordinates from inspector_snapshot.json.
 */
public final class EnemySpawnLoader {

    private static final String SNAPSHOT = "inspector_snapshot.json";

    private EnemySpawnLoader() {}

    public static List<EnemySpawn> load() {
        FileHandle file = locateSnapshot();
        List<EnemySpawn> spawns = new ArrayList<>();
        if (file == null || !file.exists()) {
            return spawns;
        }
        try {
            JsonValue root = new JsonReader().parse(file);
            JsonValue enemies = root.get("enemies");
            if (enemies == null) {
                return spawns;
            }
            for (JsonValue enemyValue : enemies) {
                String name = enemyValue.getString("name", null);
                JsonValue rowValue = enemyValue.get("row");
                JsonValue colValue = enemyValue.get("col");
                if (name == null || rowValue == null || colValue == null) {
                    continue;
                }
                spawns.add(new EnemySpawn(name, rowValue.asInt(), colValue.asInt()));
            }
        } catch (Exception exception) {
            Gdx.app.error("EnemySpawnLoader", "Failed to parse snapshot for enemies", exception);
        }
        return spawns;
    }

    private static FileHandle locateSnapshot() {
        if (Gdx.files == null) {
            return null;
        }
        FileHandle local = Gdx.files.local(SNAPSHOT);
        if (local.exists()) {
            return local;
        }
        FileHandle internal = Gdx.files.internal(SNAPSHOT);
        if (internal.exists()) {
            return internal;
        }
        return null;
    }
}


