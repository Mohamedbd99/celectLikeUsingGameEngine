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
    private static final String ENEMY_SPAWNS = "enemy_spawns.json";

    private EnemySpawnLoader() {}

    public static List<EnemySpawn> load() {
        List<EnemySpawn> spawns = new ArrayList<>();

        // 1) Prefer standalone enemy_spawns.json written by ViewEditor
        FileHandle enemyFile = locate(ENEMY_SPAWNS);
        if (enemyFile != null && enemyFile.exists()) {
            loadFromFile(enemyFile, spawns);
            if (!spawns.isEmpty()) {
                return spawns;
            }
        }

        // 2) Fallback to legacy inspector_snapshot.json enemies section
        FileHandle snapshot = locate(SNAPSHOT);
        if (snapshot != null && snapshot.exists()) {
            loadFromFile(snapshot, spawns);
        }
        return spawns;
    }

    private static FileHandle locate(String path) {
        if (Gdx.files == null) {
            return null;
        }
        FileHandle local = Gdx.files.local(path);
        if (local.exists()) {
            return local;
        }
        FileHandle internal = Gdx.files.internal(path);
        if (internal.exists()) {
            return internal;
        }
        return null;
    }

    private static void loadFromFile(FileHandle file, List<EnemySpawn> spawns) {
        try {
            JsonValue root = new JsonReader().parse(file);
            JsonValue enemies;
            if (root.isArray()) {
                // New format: root is a plain array of enemy objects
                enemies = root;
            } else {
                // Legacy format: { ..., "enemies": [ ... ] }
                enemies = root.get("enemies");
            }
            if (enemies == null) {
                return;
            }
            for (JsonValue enemyValue : enemies) {
                if (!enemyValue.isObject()) {
                    continue;
                }
                String name = enemyValue.getString("name", null);
                JsonValue rowValue = enemyValue.get("row");
                JsonValue colValue = enemyValue.get("col");
                if (name == null || rowValue == null || colValue == null) {
                    continue;
                }
                spawns.add(new EnemySpawn(name, rowValue.asInt(), colValue.asInt()));
            }
        } catch (Exception exception) {
            Gdx.app.error("EnemySpawnLoader", "Failed to parse enemy spawns from " + file.path(), exception);
        }
    }
}


