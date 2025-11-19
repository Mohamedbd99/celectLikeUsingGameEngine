package org.example.samurai;

import com.badlogic.gdx.utils.Logger;

/**
 * Command that drives the Samurai to move left at the default run speed magnitude.
 */
public final class MoveLeftCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("MoveLeftCommand", Logger.INFO);

    private boolean active;

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.moveLeft();
        if (!active) {
            LOGGER.info("MoveLeftCommand engaged");
            active = true;
        }
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        if (!active) {
            return;
        }
        samurai.stopHorizontalMovement();
        LOGGER.info("MoveLeftCommand released");
        active = false;
    }
}

