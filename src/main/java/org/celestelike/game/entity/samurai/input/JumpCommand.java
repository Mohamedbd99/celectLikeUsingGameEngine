package org.celestelike.game.entity.samurai.input;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Command that initiates a Samurai jump when executed.
 */
public final class JumpCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("JumpCommand", Logger.INFO);

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        boolean jumped = samurai.jump();
        if (!jumped) {
            jumped = samurai.wallJump();
            if (jumped) {
                LOGGER.info("Wall jump executed");
            } else {
                LOGGER.info("Jump command ignored (no available jump)");
            }
            return;
        }
        LOGGER.info("Jump command executed successfully");
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        // No-op; jump is an instantaneous command
    }
}
