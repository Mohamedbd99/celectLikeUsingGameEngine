package org.celestelike.game.entity.samurai.attack;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Finisher slash for ground combo.
 */
public final class GroundAttackThreeStrategy implements SamuraiAttackStrategy {

    private static final int DAMAGE = 15;

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.ATTACK_3;
    }

    @Override
    public boolean canExecute(SamuraiCharacter samurai) {
        return samurai.isGrounded();
    }

    @Override
    public void onEnter(SamuraiCharacter samurai) {
        samurai.stopHorizontalMovementForAttack();
    }

    @Override
    public int damage() {
        return DAMAGE;
    }
}


