package org.celestelike.game.entity.enemy;

/**
 * Basic stat container for enemies so new variants can be introduced without touching core logic.
 */
public final class EnemyStats {

    private final int maxHealth;
    private final float contactRadius;

    public EnemyStats(int maxHealth, float contactRadius) {
        this.maxHealth = Math.max(1, maxHealth);
        this.contactRadius = contactRadius;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public float contactRadius() {
        return contactRadius;
    }
}


