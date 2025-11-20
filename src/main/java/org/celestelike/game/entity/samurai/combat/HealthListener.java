package org.celestelike.game.entity.samurai.combat;

/**
 * Observer for health component events.
 */
public interface HealthListener {

    default void onHealthChanged(int current, int max) {}

    default void onDamageTaken(int amount, int current, int max) {}

    default void onHealed(int amount, int current, int max) {}

    default void onDeath() {}
}


