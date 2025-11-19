package org.example.samurai;

import com.badlogic.gdx.utils.Logger;

/**
 * Command that initiates a Samurai jump when executed.
 */
public final class JumpCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("JumpCommand", Logger.INFO);

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        boolean jumped = samurai.jump();
        if (jumped) {
            LOGGER.info("Jump command executed successfully");
        } else {
            LOGGER.info("Jump command ignored (already airborne)");
        }
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        // No-op; jump is an instantaneous command
    }
}

