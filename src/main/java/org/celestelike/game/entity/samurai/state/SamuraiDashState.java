package org.celestelike.game.entity.samurai.state;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * State representing an active dash burst.
 */
public final class SamuraiDashState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiDashState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering DASH state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai dashing (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.DASH;
    }

    @Override
    public String name() {
        return "DASH";
    }
}


