package org.celestelike.game.entity.samurai.input;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

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

