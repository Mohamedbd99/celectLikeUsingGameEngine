package org.celestelike.game.entity.samurai.state;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * State representing an airborne Samurai during a jump.
 */
public final class SamuraiJumpState implements SamuraiState {

    private static final Logger LOGGER = new Logger("SamuraiJumpState", Logger.INFO);

    @Override
    public void enter(SamuraiCharacter samurai) {
        LOGGER.info("Entering JUMP state");
    }

    @Override
    public void update(SamuraiCharacter samurai, float delta) {
        if (LOGGER.getLevel() <= Logger.DEBUG) {
            LOGGER.debug("Samurai airborne (delta=" + delta + ")");
        }
    }

    @Override
    public SamuraiAnimationKey animationKey() {
        return SamuraiAnimationKey.JUMP;
    }

    @Override
    public String name() {
        return "JUMP";
    }
}

