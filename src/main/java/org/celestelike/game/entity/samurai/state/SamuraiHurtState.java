package org.celestelike.game.entity.samurai.state;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Transitional hurt flinch state (e.g., when touching lethal hazards).
 */
public final class SamuraiHurtState implements SamuraiState {

    @Override
    public void enter(SamuraiCharacter samurai) {
        samurai.stopHorizontalMovementForAttack();
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        // SamuraiCharacter handles timing and transition to death
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.HURT;
    }

    @Override
    public String name() {
        return "HURT";
    }
}


