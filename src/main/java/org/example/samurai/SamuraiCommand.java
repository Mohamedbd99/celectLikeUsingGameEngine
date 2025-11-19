package org.example.samurai;

/**
 * Command abstraction for Samurai actions (Command pattern).
 */
public interface SamuraiCommand {

    /**
     * Executes the command (typically while an input is held).
     *
     * @param samurai character instance
     * @param delta   elapsed seconds
     */
    void execute(SamuraiCharacter samurai, float delta);

    /**
     * Called when the command should be released (e.g., key up).
     *
     * @param samurai character instance
     */
    void release(SamuraiCharacter samurai);
}

