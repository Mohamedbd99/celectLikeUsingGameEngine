package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

final class EnemyLeafNode implements EnemyComponent {

    private final EnemyInstance instance;

    EnemyLeafNode(EnemyInstance instance) {
        this.instance = instance;
    }

    @Override
    public void update(float delta) {
        instance.update(delta);
    }
    
    public void update(float delta, Vector2 playerPosition) {
        instance.update(delta, playerPosition);
    }
    
    public void checkAttack(Vector2 playerPosition, Object player) {
        if (instance.isDead() || !instance.isAttacking()) {
            return;
        }
        if (!instance.canDealDamage()) {
            return;
        }
        // Check if player is in attack range
        float centerX = instance.position().x + instance.width() * 0.5f;
        float centerY = instance.position().y + instance.height() * 0.5f;
        float distance = playerPosition.dst(centerX, centerY);
        if (distance <= instance.definition().stats().attackRange()) {
            // Apply damage to player (10% of max health) - attack completes even if defending
            if (player instanceof org.celestelike.game.entity.samurai.SamuraiCharacter) {
                org.celestelike.game.entity.samurai.SamuraiCharacter samurai = 
                    (org.celestelike.game.entity.samurai.SamuraiCharacter) player;
                
                // Only apply damage if player is NOT defending
                // Attack animation still completes, but no damage is dealt when defending
                if (!samurai.isDefending()) {
                    // Calculate 10% of max health as damage
                    int maxHealth = samurai.getMaxHealth();
                    int damage = Math.max(1, (int) Math.ceil(maxHealth * 0.1f));
                    samurai.applyDamage(damage);
                }
                // If defending, attack still "hits" but no damage is applied
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        instance.draw(batch);
    }

    @Override
    public void drawHealthFill(ShapeRenderer renderer) {
        instance.drawHealthBarFill(renderer);
    }

    @Override
    public void drawHealthOutline(ShapeRenderer renderer) {
        instance.drawHealthBarOutline(renderer);
    }

    @Override
    public boolean applyMeleeDamage(
            Vector2 attackPoint,
            float attackRadius,
            int damage,
            EnemyManager.EnemyEventListener listener) {
        if (instance.isDead()) {
            return false;
        }
        if (!isWithinRange(attackPoint, attackRadius)) {
            return false;
        }
        boolean defeated = instance.applyDamage(damage);
        if (defeated && listener != null) {
            listener.onEnemyDefeated(instance.definition());
        }
        return true;
    }

    private boolean isWithinRange(Vector2 point, float range) {
        float centerX = instance.position().x + instance.width() * 0.5f;
        float centerY = instance.position().y + instance.height() * 0.5f;
        float distance2 = point.dst2(centerX, centerY);
        float radius = instance.radius() + range;
        return distance2 <= radius * radius;
    }

    @Override
    public void dispose() {
        instance.dispose();
    }

    @Override
    public boolean isEmpty() {
        return instance.isDead();
    }
}

