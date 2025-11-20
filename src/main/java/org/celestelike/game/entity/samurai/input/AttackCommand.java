package org.celestelike.game.entity.samurai.input;

import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Triggers the Samurai's combo attack sequence.
 */
public final class AttackCommand implements SamuraiCommand {

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.attack();
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        // attacks are executed on press only
    }
}


