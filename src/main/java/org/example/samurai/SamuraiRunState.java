package org.example.samurai;

import com.badlogic.gdx.utils.Logger;

/**
 * State representing the Samurai running horizontally.
 */
public final class SamuraiRunState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiRunState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering RUN state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai running (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.RUN;
    }

    @Override
    public String name() {
        return "RUN";
    }
}

