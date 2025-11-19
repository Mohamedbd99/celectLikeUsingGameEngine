package org.celestelike.game.entity.samurai.state;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Default standing state for the Samurai.
 */
public final class SamuraiIdleState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiIdleState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering IDLE state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        // No behavior yet, but we still emit a trace when verbose logging is enabled.
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai remains idle (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.IDLE;
    }

    @Override
    public String name() {
        return "IDLE";
    }
}

