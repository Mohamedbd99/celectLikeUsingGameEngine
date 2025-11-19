package org.celestelike.game.entity.samurai.input;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Command that sets the Samurai's upward intent (used for aiming jumps/dashes).
 */
public final class MoveUpCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("MoveUpCommand", Logger.INFO);

    private boolean active;

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.aimUp();
        if (!active) {
            LOGGER.info("MoveUpCommand engaged");
            active = true;
        }
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        if (!active) {
            return;
        }
        samurai.clearVerticalIntent();
        LOGGER.info("MoveUpCommand released");
        active = false;
    }
}

