package org.celestelike.game.entity.samurai.state;

import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * State interface for the Samurai hero.
 * Implementations encapsulate the behavior for a specific player state.
 */
public interface SamuraiState {

    /**
     * Called whenever the Samurai enters this state.
     *
     * @param samurai the owning character
     */
    void enter(SamuraiCharacter samurai);

    /**
     * Called each frame while the Samurai remains in this state.
     *
     * @param samurai the owning character
     * @param delta   elapsed time in seconds
     */
    void update(SamuraiCharacter samurai, float delta);

    /**
     * @return the animation key associated with this state
     */
    SamuraiAnimationKey animationKey();

    /**
     * @return human-readable name, useful for logger output
     */
    String name();
}

