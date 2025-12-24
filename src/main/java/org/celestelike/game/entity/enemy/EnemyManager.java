package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

public final class EnemyManager {

    private static final float ATTACK_RANGE = 38f;
    private final EnemyGroupNode rootGroup = new EnemyGroupNode("root");
    private final Map<String, EnemyGroupNode> groupedById = new HashMap<>();
    private final Vector2 attackPoint = new Vector2();
    private EnemyEventListener eventListener;

    public EnemyManager() {
        EnemyRegistry.registerDefaults();
    }

    public void spawnAll(List<EnemySpawn> spawns, int totalRows, float tileWorldSize) {
        for (EnemySpawn spawn : spawns) {
            EnemyDefinition definition = EnemyRegistry.definition(spawn.id());
            if (definition == null) {
                continue;
            }
            // Editor and collision map now treat row 0 as the BOTTOM row,
            // so enemies should spawn with the same convention.
            float x = spawn.col() * tileWorldSize;
            float y = spawn.row() * tileWorldSize;
            EnemyInstance instance = new EnemyInstance(definition, x, y);
            EnemyLeafNode leaf = new EnemyLeafNode(instance);
            EnemyGroupNode bucket = groupedById.computeIfAbsent(definition.id(), id -> {
                EnemyGroupNode node = new EnemyGroupNode(id);
                rootGroup.add(node);
                return node;
            });
            bucket.add(leaf);
        }
    }

    public void update(float delta) {
        rootGroup.update(delta);
    }
    
    public void update(float delta, Vector2 playerPosition) {
        if (playerPosition != null) {
            updateEnemiesWithPlayer(delta, playerPosition);
        } else {
            rootGroup.update(delta);
        }
    }
    
    private void updateEnemiesWithPlayer(float delta, Vector2 playerPosition) {
        rootGroup.updateWithPlayer(delta, playerPosition);
    }
    
    public void checkEnemyAttacks(SamuraiCharacter samurai) {
        if (samurai == null || samurai.isDead() || rootGroup.isEmpty()) {
            return;
        }
        Vector2 playerPos = samurai.getPosition();
        rootGroup.checkEnemyAttacks(playerPos, samurai);
    }

    public void draw(SpriteBatch batch) {
        rootGroup.draw(batch);
    }

    public void drawHealthBars(ShapeRenderer shapeRenderer) {
        if (rootGroup.isEmpty()) {
            return;
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        rootGroup.drawHealthFill(shapeRenderer);
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        rootGroup.drawHealthOutline(shapeRenderer);
        shapeRenderer.end();
    }

    public void applyMeleeDamage(SamuraiCharacter samurai, int damage) {
        if (damage <= 0 || rootGroup.isEmpty()) {
            return;
        }
        attackPoint.set(samurai.getPosition());
        attackPoint.x += samurai.isFacingRight() ? 68f : -68f;
        attackPoint.y += 32f;

        rootGroup.applyMeleeDamage(attackPoint, ATTACK_RANGE, damage, eventListener);
    }

    public void dispose() {
        rootGroup.dispose();
        groupedById.clear();
    }

    public boolean isEmpty() {
        return rootGroup.isEmpty();
    }

    public void respawn(List<EnemySpawn> spawns, int rows, float tileWorldSize) {
        dispose();
        if (spawns == null || spawns.isEmpty()) {
            return;
        }
        spawnAll(spawns, rows, tileWorldSize);
    }

    public void setEventListener(EnemyEventListener listener) {
        this.eventListener = listener;
    }

    public interface EnemyEventListener {
        void onEnemyDefeated(EnemyDefinition definition);
    }
}


