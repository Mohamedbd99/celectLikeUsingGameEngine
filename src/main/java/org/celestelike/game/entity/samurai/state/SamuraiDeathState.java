package org.celestelike.game.entity.samurai.state;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Plays the death animation sequence.
 */
public final class SamuraiDeathState implements SamuraiState {

    @Override
    public void enter(SamuraiCharacter samurai) {
        samurai.stopHorizontalMovementForAttack();
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        // Animation timing handled by SamuraiCharacter.tickDeath
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.DEATH;
    }

    @Override
    public String name() {
        return "DEATH";
    }
}


