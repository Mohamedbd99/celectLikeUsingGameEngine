package org.celestelike.game.entity.samurai.input;

import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Command that triggers the Samurai dash burst.
 */
public final class DashCommand implements SamuraiCommand {

    private static final Logger LOGGER = new Logger("DashCommand", Logger.INFO);
    private float directionX;
    private float directionY;

    public void setDirection(float directionX, float directionY) {
        this.directionX = directionX;
        this.directionY = directionY;
    }

    @Override
    public void execute(SamuraiCharacter samurai, float delta) {
        boolean dashed = samurai.dash(directionX, directionY);
        if (dashed) {
            LOGGER.info("Dash command executed successfully");
        } else {
            LOGGER.info("Dash command ignored (dash unavailable)");
        }
    }

    @Override
    public void release(SamuraiCharacter samurai) {
        // Dash is an instantaneous burst; nothing to release.
    }
}


