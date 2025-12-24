package org.celestelike.game.entity.samurai.powerup;

/**
 * Describes the mutable combat/movement attributes for the samurai hero.
 * Decorators can wrap this interface to stack temporary modifiers.
 */
public interface SamuraiAttributes {

    default float attackMultiplier() {
        return 1f;
    }

    default int attackBonus() {
        return 0;
    }

    default float defenseMultiplier() {
        return 1f;
    }

    default float speedMultiplier() {
        return 1f;
    }
}

