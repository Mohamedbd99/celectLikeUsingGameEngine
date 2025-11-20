package org.celestelike.game.entity.samurai.state;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Simple one-off state for the special attack animation.
 */
public final class SamuraiSpecialAttackState implements SamuraiState {

    @Override
    public void enter(SamuraiCharacter samurai) {
        samurai.stopHorizontalMovementForAttack();
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        // Animation playback handled directly in SamuraiCharacter.
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.SPECIAL_ATTACK;
    }

    @Override
    public String name() {
        return "SPECIAL_ATTACK";
    }
}


