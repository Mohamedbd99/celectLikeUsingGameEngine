package org.celestelike.game.entity.samurai.state;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * State used while the Samurai slides down a wall.
 */
public final class SamuraiWallSlideState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiWallSlideState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering WALL SLIDE state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai wall-sliding (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.WALL_SLIDE;
    }

    @Override
    public String name() {
        return "WALL_SLIDE";
    }
}

