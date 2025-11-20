package org.celestelike.game.entity.samurai.attack;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Mid-air downward slash.
 */
public final class AirAttackStrategy implements SamuraiAttackStrategy {

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.AIR_ATTACK;
    }

    @Override
    public boolean canExecute(SamuraiCharacter samurai) {
        return !samurai.isGrounded();
    }
}


