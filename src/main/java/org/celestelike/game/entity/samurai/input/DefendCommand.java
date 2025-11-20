package org.celestelike.game.entity.samurai.input;

import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Holds the samurai in a defensive stance while the input is pressed.
 */
public final class DefendCommand implements SamuraiCommand {

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.startDefend();
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        samurai.stopDefend();
    }
}


