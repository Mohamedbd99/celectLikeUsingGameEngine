package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import java.util.Iterator;
import java.util.List;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

public final class EnemyManager {

    private static final float ATTACK_RANGE = 96f;
    private final Array<EnemyInstance> active = new Array<>();
    private final Vector2 attackPoint = new Vector2();

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
            active.add(new EnemyInstance(definition, x, y));
        }
    }

    public void update(float delta) {
        for (EnemyInstance enemy : active) {
            enemy.update(delta);
        }
        purgeDead();
    }

    private void purgeDead() {
        for (Iterator<EnemyInstance> iterator = active.iterator(); iterator.hasNext();) {
            EnemyInstance enemy = iterator.next();
            if (enemy.isDead()) {
                enemy.dispose();
                iterator.remove();
            }
        }
    }

    public void draw(SpriteBatch batch) {
        for (EnemyInstance enemy : active) {
            enemy.draw(batch);
        }
    }

    public void drawHealthBars(ShapeRenderer shapeRenderer) {
        if (active.isEmpty()) {
            return;
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (EnemyInstance enemy : active) {
            enemy.drawHealthBarFill(shapeRenderer);
        }
        shapeRenderer.end();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (EnemyInstance enemy : active) {
            enemy.drawHealthBarOutline(shapeRenderer);
        }
        shapeRenderer.end();
    }

    public void applyMeleeDamage(SamuraiCharacter samurai, int damage) {
        if (damage <= 0 || active.isEmpty()) {
            return;
        }
        attackPoint.set(samurai.getPosition());
        attackPoint.x += samurai.isFacingRight() ? 70f : -20f;
        attackPoint.y += 40f;

        for (EnemyInstance enemy : active) {
            if (enemy.isDead()) {
                continue;
            }
            if (isWithinRange(enemy, attackPoint)) {
                enemy.applyDamage(damage);
                break;
            }
        }
    }

    private boolean isWithinRange(EnemyInstance enemy, Vector2 point) {
        float centerX = enemy.position().x + enemy.width() * 0.5f;
        float centerY = enemy.position().y + enemy.height() * 0.5f;
        float distance2 = point.dst2(centerX, centerY);
        float radius = enemy.radius() + ATTACK_RANGE;
        return distance2 <= radius * radius;
    }

    public void dispose() {
        for (EnemyInstance enemy : active) {
            enemy.dispose();
        }
        active.clear();
    }

    public boolean isEmpty() {
        return active.isEmpty();
    }

    public void respawn(List<EnemySpawn> spawns, int rows, float tileWorldSize) {
        dispose();
        if (spawns == null || spawns.isEmpty()) {
            return;
        }
        spawnAll(spawns, rows, tileWorldSize);
    }
}


