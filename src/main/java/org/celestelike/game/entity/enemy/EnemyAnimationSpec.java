package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.graphics.g2d.Animation;

/**
 * Describes how to build a LibGDX animation from a spritesheet.
 */
public final class EnemyAnimationSpec {

    private final String file;
    private final int frameWidth;
    private final int frameHeight;
    private final float frameDuration;
    private final Animation.PlayMode playMode;

    public EnemyAnimationSpec(String file, int frameWidth, int frameHeight, float frameDuration, Animation.PlayMode playMode) {
        this.file = file;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameDuration = frameDuration;
        this.playMode = playMode;
    }

    public String file() {
        return file;
    }

    public int frameWidth() {
        return frameWidth;
    }

    public int frameHeight() {
        return frameHeight;
    }

    public float frameDuration() {
        return frameDuration;
    }

    public Animation.PlayMode playMode() {
        return playMode;
    }

    public static EnemyAnimationSpec looping(String file, int frameWidth, int frameHeight, float frameDuration) {
        return new EnemyAnimationSpec(file, frameWidth, frameHeight, frameDuration, Animation.PlayMode.LOOP);
    }

    public static EnemyAnimationSpec once(String file, int frameWidth, int frameHeight, float frameDuration) {
        return new EnemyAnimationSpec(file, frameWidth, frameHeight, frameDuration, Animation.PlayMode.NORMAL);
    }
}


