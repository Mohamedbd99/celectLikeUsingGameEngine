package org.celestelike.game.entity.samurai.powerup;

/**
 * Increases outgoing damage.
 */
public final class WeaponPowerUp extends PowerUpDecorator {

    private final float multiplier;
    private final int bonus;

    public WeaponPowerUp(SamuraiAttributes delegate, float multiplier, int bonus) {
        super(delegate);
        this.multiplier = multiplier;
        this.bonus = bonus;
    }

    @Override
    public float attackMultiplier() {
        return delegate.attackMultiplier() * multiplier;
    }

    @Override
    public int attackBonus() {
        return delegate.attackBonus() + bonus;
    }
}

