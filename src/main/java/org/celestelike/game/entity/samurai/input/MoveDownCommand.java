package org.celestelike.game.entity.samurai.input;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Command that sets the Samurai's downward intent (for aiming future moves).
 */
public final class MoveDownCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("MoveDownCommand", Logger.INFO);

    private boolean active;

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        samurai.aimDown();
        if (!active) {
            LOGGER.info("MoveDownCommand engaged");
            active = true;
        }
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        if (!active) {
            return;
        }
        samurai.clearVerticalIntent();
        LOGGER.info("MoveDownCommand released");
        active = false;
    }
}

