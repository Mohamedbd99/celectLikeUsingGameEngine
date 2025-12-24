package org.celestelike.game.entity.samurai.powerup;

import java.util.function.Function;

/**
 * Available power-up definitions. Each enum entry knows how to wrap the
 * current attribute chain.
 */
public enum SamuraiPowerUpType {
    SHIELD("Aegis Shield", 15f, attrs -> new ShieldPowerUp(attrs, 0.6f)),
    SPEED("Windrunner Boots", 12f, attrs -> new SpeedPowerUp(attrs, 1.35f)),
    WEAPON("Crimson Blade", 10f, attrs -> new WeaponPowerUp(attrs, 1.4f, 6));

    private final String displayName;
    private final float durationSeconds;
    private final Function<SamuraiAttributes, SamuraiAttributes> decoratorFactory;

    SamuraiPowerUpType(
            String displayName,
            float durationSeconds,
            Function<SamuraiAttributes, SamuraiAttributes> decoratorFactory) {
        this.displayName = displayName;
        this.durationSeconds = durationSeconds;
        this.decoratorFactory = decoratorFactory;
    }

    public String displayName() {
        return displayName;
    }

    public float durationSeconds() {
        return durationSeconds;
    }

    public SamuraiAttributes wrap(SamuraiAttributes current) {
        return decoratorFactory.apply(current);
    }
}

