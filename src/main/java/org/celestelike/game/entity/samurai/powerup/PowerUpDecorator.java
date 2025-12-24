package org.celestelike.game.entity.samurai.powerup;

/**
 * Base decorator that forwards to the wrapped attributes.
 */
public abstract class PowerUpDecorator implements SamuraiAttributes {

    protected final SamuraiAttributes delegate;

    protected PowerUpDecorator(SamuraiAttributes delegate) {
        this.delegate = delegate;
    }

    @Override
    public float attackMultiplier() {
        return delegate.attackMultiplier();
    }

    @Override
    public int attackBonus() {
        return delegate.attackBonus();
    }

    @Override
    public float defenseMultiplier() {
        return delegate.defenseMultiplier();
    }

    @Override
    public float speedMultiplier() {
        return delegate.speedMultiplier();
    }
}

