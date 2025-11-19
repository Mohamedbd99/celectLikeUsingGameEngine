package org.celestelike.game.entity.samurai.state;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * State used when the Samurai is clinging to a wall without sliding.
 */
public final class SamuraiWallContactState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiWallContactState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering WALL CONTACT state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai clinging to wall (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.WALL_CONTACT;
    }

    @Override
    public String name() {
        return "WALL_CONTACT";
    }
}

