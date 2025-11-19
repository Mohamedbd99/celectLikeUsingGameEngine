package org.example.samurai;

import com.badlogic.gdx.utils.Logger;

/**
 * Command that drives the Samurai to move right at the default run speed.
 */
public final class MoveRightCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("MoveRightCommand", Logger.INFO);

    private boolean active;

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.moveRight();
        if (!active) {
            LOGGER.info("MoveRightCommand engaged");
            active = true;
        }
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        if (!active) {
            return;
        }
        samurai.stopHorizontalMovement();
        LOGGER.info("MoveRightCommand released");
        active = false;
    }
}

