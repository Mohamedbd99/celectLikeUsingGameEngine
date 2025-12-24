package org.celestelike.game.entity.samurai.powerup;

/**
 * Boosts horizontal movement, jump, and dash speeds.
 */
public final class SpeedPowerUp extends PowerUpDecorator {

    private final float multiplier;

    public SpeedPowerUp(SamuraiAttributes delegate, float multiplier) {
        super(delegate);
        this.multiplier = multiplier;
    }

    @Override
    public float speedMultiplier() {
        return delegate.speedMultiplier() * multiplier;
    }
}

