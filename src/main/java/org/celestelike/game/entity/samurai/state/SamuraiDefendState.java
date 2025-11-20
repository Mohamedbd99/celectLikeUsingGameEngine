package org.celestelike.game.entity.samurai.state;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Defensive stance state; loops defensive animation while active.
 */
public final class SamuraiDefendState implements SamuraiState {

    @Override
    public void enter(SamuraiCharacter samurai) {
        samurai.stopHorizontalMovementForAttack();
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        // No-op: defensive stance is driven externally by start/stop calls.
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.DEFEND;
    }

    @Override
    public String name() {
        return "DEFEND";
    }
}


