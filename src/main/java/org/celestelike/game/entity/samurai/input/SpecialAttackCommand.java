package org.celestelike.game.entity.samurai.input;

import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Triggers the special attack animation.
 */
public final class SpecialAttackCommand implements SamuraiCommand {

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.specialAttack();
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        // one-shot ability; no release handling needed
    }
}


