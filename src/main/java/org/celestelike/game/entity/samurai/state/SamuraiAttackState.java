package org.celestelike.game.entity.samurai.state;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;
import org.celestelike.game.entity.samurai.attack.SamuraiAttackStrategy;

/**
 * Generic attack state that delegates behavior to an attack strategy.
 */
public final class SamuraiAttackState implements SamuraiState {

    private SamuraiAttackStrategy strategy;

    public void setStrategy(SamuraiAttackStrategy strategy) {
        this.strategy = strategy;
    }

    public void clearStrategy() {
        this.strategy = null;
    }

    @Override
    public void enter(SamuraiCharacter samurai) {
        if (strategy != null) {
            strategy.onEnter(samurai);
        }
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (strategy == null) {
            samurai.onAttackStateComplete();
            return;
        }
        strategy.onUpdate(samurai, delta);
        if (strategy.shouldEnd(samurai)) {
            strategy.onExit(samurai);
            clearStrategy();
            samurai.onAttackStateComplete();
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return strategy != null ? strategy.animationKey() : SamuraiAnimationKey.IDLE;
    }

    @Override
    public String name() {
        return "ATTACK";
    }
}


