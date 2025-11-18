package org.example.samurai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Samurai hero facade. Handles loading, state updates, animation selection, and rendering.
 */
public final class SamuraiCharacter {

    private static final Logger LOGGER = new Logger("SamuraiCharacter", Logger.INFO);

    private static final String SAMURAI_BASE = "assets/FREE_Samurai 2D Pixel Art v1.2/Sprites/";
    private static final String IDLE_FILE = SAMURAI_BASE + "IDLE.png";
    private static final int FRAME_SIZE = 96;
    private static final float DEFAULT_FRAME_DURATION = 0.08f;

    private final EnumMap<SamuraiAnimationKey, Animation<TextureRegion>> animations =
            new EnumMap<>(SamuraiAnimationKey.class);
    private final List<Texture> ownedTextures = new ArrayList<>();
    private final SamuraiIdleState idleState = new SamuraiIdleState();

    private SamuraiState currentState;
    private TextureRegion currentFrame;
    private float x;
    private float y;
    private float stateTime;

    public SamuraiCharacter() {
        LOGGER.info("Samurai character initialized");
    }

    public void loadAssets() {
        LOGGER.info("Loading samurai spritesheets");
        Animation<TextureRegion> idleAnimation = loadAnimation(IDLE_FILE, DEFAULT_FRAME_DURATION);
        if (idleAnimation != null) {
            animations.put(SamuraiAnimationKey.IDLE, idleAnimation);
            currentFrame = idleAnimation.getKeyFrame(0f);
            LOGGER.info("Idle animation primed");
        } else {
            LOGGER.error("Failed to load idle animation; rendering will be skipped.");
        }
    }

    private Animation<TextureRegion> loadAnimation(String path, float frameDuration) {
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            LOGGER.error("Missing animation sheet: " + path);
            return null;
        }
        Texture texture = new Texture(handle);
        ownedTextures.add(texture);
        TextureRegion[][] split = TextureRegion.split(texture, FRAME_SIZE, FRAME_SIZE);
        Array<TextureRegion> frames = new Array<>();
        for (TextureRegion[] row : split) {
            for (TextureRegion region : row) {
                if (region == null) {
                    continue;
                }
                frames.add(region);
            }
        }
        if (frames.isEmpty()) {
            LOGGER.error("Spritesheet had no frames: " + path);
            return null;
        }
        LOGGER.info("Loaded " + frames.size + " frames from " + path);
        return new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
    }

    public void placeAt(float worldX, float worldY) {
        this.x = worldX;
        this.y = worldY;
        LOGGER.info("Samurai placed at (" + worldX + ", " + worldY + ")");
    }

    public void switchState(SamuraiState nextState) {
        if (nextState == null) {
            LOGGER.error("Attempted to switch to a null state");
            return;
        }
        String prev = currentState == null ? "NONE" : currentState.name();
        LOGGER.info("Switching state from " + prev + " to " + nextState.name());
        currentState = nextState;
        stateTime = 0f;
        nextState.enter(this);
    }

    public void ensureIdleState() {
        if (currentState == null || currentState != idleState) {
            switchState(idleState);
        }
    }

    public void update(float delta) {
        stateTime += delta;
        if (currentState == null) {
            LOGGER.error("Samurai has no active state; invoking ensureIdleState");
            ensureIdleState();
        }
        if (currentState != null) {
            currentState.update(this, delta);
            Animation<TextureRegion> animation = animations.get(currentState.animationKey());
            if (animation != null) {
                currentFrame = animation.getKeyFrame(stateTime, true);
            } else {
                LOGGER.error("No animation registered for state " + currentState.name());
            }
        }
    }

    public void draw(SpriteBatch batch) {
        if (currentFrame == null) {
            LOGGER.error("Samurai current frame is null; skipping draw");
            return;
        }
        batch.draw(currentFrame, x, y);
    }

    public void dispose() {
        LOGGER.info("Disposing samurai textures");
        for (Texture texture : ownedTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }
        ownedTextures.clear();
    }

    public Animation<TextureRegion> animationFor(SamuraiAnimationKey key) {
        return animations.get(key);
    }

    public float getStateTime() {
        return stateTime;
    }

    public void resetStateTime() {
        this.stateTime = 0f;
    }

    public SamuraiState idleState() {
        return idleState;
    }
}

