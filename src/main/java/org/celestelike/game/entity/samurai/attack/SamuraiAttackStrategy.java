package org.celestelike.game.entity.samurai.attack;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import org.celestelike.game.entity.samurai.SamuraiAnimationKey;
import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Strategy definition for Samurai attacks. Each attack encapsulates
 * its own entry/exit behavior and animation hooks.
 */
public interface SamuraiAttackStrategy {

    /**
     * @return animation key that should be displayed while this attack is active
     */
    SamuraiAnimationKey animationKey();

    /**
     * @return true if the strategy can execute with the current Samurai context
     */
    default boolean canExecute(SamuraiCharacter samurai) {
        return true;
    }

    /**
     * Called when the attack begins.
     */
    default void onEnter(SamuraiCharacter samurai) {}

    /**
     * Called each update while the attack is active.
     */
    default void onUpdate(SamuraiCharacter samurai, float delta) {}

    /**
     * Called when the attack finishes.
     */
    default void onExit(SamuraiCharacter samurai) {}

    /**
     * @return true when the strategy considers itself complete.
     */
    default boolean shouldEnd(SamuraiCharacter samurai) {
        Animation<TextureRegion> animation = samurai.animationFor(animationKey());
        if (animation == null) {
            return true;
        }
        return animation.isAnimationFinished(samurai.getStateTime());
    }
}


