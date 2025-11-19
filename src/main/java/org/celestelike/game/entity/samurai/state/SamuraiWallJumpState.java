package org.celestelike.game.entity.samurai.state;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Transitional state played immediately after a wall jump input.
 */
public final class SamuraiWallJumpState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiWallJumpState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering WALL JUMP state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai wall jumping (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.WALL_JUMP;
    }

    @Override
    public String name() {
        return "WALL_JUMP";
    }
}

