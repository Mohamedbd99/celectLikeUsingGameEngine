package org.celestelike.game.entity.samurai.powerup;

/**
 * Immutable projection used by the HUD.
 */
public record SamuraiPowerUpSnapshot(
        SamuraiPowerUpType type,
        float remainingSeconds) {
}

