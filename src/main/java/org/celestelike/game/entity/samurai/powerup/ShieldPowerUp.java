package org.celestelike.game.entity.samurai.powerup;

/**
 * Reduces incoming damage.
 */
public final class ShieldPowerUp extends PowerUpDecorator {

    private final float mitigation;

    public ShieldPowerUp(SamuraiAttributes delegate, float mitigation) {
        super(delegate);
        this.mitigation = mitigation;
    }

    @Override
    public float defenseMultiplier() {
        return Math.max(0.1f, delegate.defenseMultiplier() * mitigation);
    }
}

