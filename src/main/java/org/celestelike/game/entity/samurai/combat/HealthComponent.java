package org.celestelike.game.entity.samurai.combat;

import com.badlogic.gdx.utils.Array;

/**
 * Observable health container for the Samurai.
 */
public final class HealthComponent {

    private final Array<HealthListener> listeners = new Array<>();
    private final int maxHealth;
    private int currentHealth;
    private boolean dead;

    public HealthComponent(int maxHealth) {
        this.maxHealth = Math.max(1, maxHealth);
        this.currentHealth = this.maxHealth;
        this.dead = false;
    }

    public int maxHealth() {
        return maxHealth;
    }

    public int currentHealth() {
        return currentHealth;
    }

    public boolean isDead() {
        return dead;
    }

    public void addListener(HealthListener listener) {
        if (listener != null && !listeners.contains(listener, true)) {
            listeners.add(listener);
        }
    }

    public void removeListener(HealthListener listener) {
        listeners.removeValue(listener, true);
    }

    public void damage(int amount) {
        if (dead || amount <= 0) {
            return;
        }
        currentHealth = Math.max(0, currentHealth - amount);
        for (HealthListener listener : listeners) {
            listener.onDamageTaken(amount, currentHealth, maxHealth);
            listener.onHealthChanged(currentHealth, maxHealth);
        }
        if (currentHealth == 0) {
            dead = true;
            for (HealthListener listener : listeners) {
                listener.onDeath();
            }
        }
    }

    public void heal(int amount) {
        if (dead || amount <= 0) {
            return;
        }
        int previous = currentHealth;
        currentHealth = Math.min(maxHealth, currentHealth + amount);
        int healed = currentHealth - previous;
        if (healed <= 0) {
            return;
        }
        for (HealthListener listener : listeners) {
            listener.onHealed(healed, currentHealth, maxHealth);
            listener.onHealthChanged(currentHealth, maxHealth);
        }
    }

    public void kill() {
        if (dead) {
            return;
        }
        currentHealth = 0;
        dead = true;
        for (HealthListener listener : listeners) {
            listener.onHealthChanged(currentHealth, maxHealth);
            listener.onDeath();
        }
    }

    public void reset() {
        dead = false;
        currentHealth = maxHealth;
        for (HealthListener listener : listeners) {
            listener.onHealthChanged(currentHealth, maxHealth);
        }
    }
}


