package org.celestelike.game.entity.enemy;

/**
 * Basic stat container for enemies so new variants can be introduced without touching core logic.
 */
public final class EnemyStats {

    private final int maxHealth;
    private final float contactRadius;
    private final float moveSpeed;
    private final int attackDamage;
    private final float attackRange;
    private final float attackCooldown;

    public EnemyStats(int maxHealth, float contactRadius) {
        this(maxHealth, contactRadius, 30f, 15, 50f, 1.5f);
    }

    public EnemyStats(int maxHealth, float contactRadius, float moveSpeed, int attackDamage, float attackRange, float attackCooldown) {
        this.maxHealth = Math.max(1, maxHealth);
        this.contactRadius = contactRadius;
        this.moveSpeed = Math.max(0f, moveSpeed);
        this.attackDamage = Math.max(0, attackDamage);
        this.attackRange = Math.max(0f, attackRange);
        this.attackCooldown = Math.max(0f, attackCooldown);
    }

    public int maxHealth() {
        return maxHealth;
    }

    public float contactRadius() {
        return contactRadius;
    }

    public float moveSpeed() {
        return moveSpeed;
    }

    public int attackDamage() {
        return attackDamage;
    }

    public float attackRange() {
        return attackRange;
    }

    public float attackCooldown() {
        return attackCooldown;
    }
}


