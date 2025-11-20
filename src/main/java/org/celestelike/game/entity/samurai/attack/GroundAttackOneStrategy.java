package org.celestelike.game.entity.samurai.attack;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Forward slash opener.
 */
public final class GroundAttackOneStrategy implements SamuraiAttackStrategy {

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.ATTACK_1;
    }

    @Override
    public boolean canExecute(SamuraiCharacter samurai) {
        return samurai.isGrounded();
    }

    @Override
    public void onEnter(SamuraiCharacter samurai) {
        samurai.stopHorizontalMovementForAttack();
    }
}


