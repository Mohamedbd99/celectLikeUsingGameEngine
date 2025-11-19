package org.example.samurai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
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
    private static final String SAMURAI_BASE = "assets/FULL_Samurai 2D Pixel Art v1.2/Sprites/";
    private static final String IDLE_FILE = SAMURAI_BASE + "IDLE.png";
    private static final String RUN_FILE = SAMURAI_BASE + "RUN.png";
    private static final String JUMP_FILE = SAMURAI_BASE + "JUMP.png";
    private static final int FRAME_SIZE = 96;
    private static final float DEFAULT_FRAME_DURATION = 0.08f;
    private static final float RUN_FRAME_DURATION = 0.05f;
    private static final float JUMP_FRAME_DURATION = 0.06f;
    private static final float DEFAULT_RUN_SPEED = 220f;
    private static final float DEFAULT_JUMP_SPEED = 620f;

    private final EnumMap<SamuraiAnimationKey, Animation<TextureRegion>> animations =
            new EnumMap<>(SamuraiAnimationKey.class);
    private final List<Texture> ownedTextures = new ArrayList<>();
    private final SamuraiIdleState idleState = new SamuraiIdleState();
    private final SamuraiRunState runState = new SamuraiRunState();
    private final SamuraiJumpState jumpState = new SamuraiJumpState();
    private final SamuraiKinematicController controller = new SamuraiKinematicController();

    private SamuraiState currentState;
    private TextureRegion currentFrame;
    private float stateTime;
    private int verticalIntent; // -1 down, 0 neutral, 1 up
    private boolean facingRight = true;
    private float jumpSpeed = DEFAULT_JUMP_SPEED;

    public SamuraiCharacter() {
        LOGGER.info("Samurai character initialized");
    }

    public void loadAssets() {
        LOGGER.info("Loading samurai spritesheets");
        Animation<TextureRegion> idleAnimation = loadAnimation(IDLE_FILE, DEFAULT_FRAME_DURATION);
        Animation<TextureRegion> runAnimation = loadAnimation(RUN_FILE, RUN_FRAME_DURATION);
        Animation<TextureRegion> jumpAnimation = loadAnimation(JUMP_FILE, JUMP_FRAME_DURATION);

        if (idleAnimation != null) {
            animations.put(SamuraiAnimationKey.IDLE, idleAnimation);
            currentFrame = idleAnimation.getKeyFrame(0f);
            LOGGER.info("Idle animation primed");
        } else {
            LOGGER.error("Failed to load idle animation; rendering will be skipped.");
        }

        if (runAnimation != null) {
            animations.put(SamuraiAnimationKey.RUN, runAnimation);
            LOGGER.info("Run animation primed");
        } else {
            LOGGER.error("Failed to load run animation; run state will fallback to idle frames.");
        }

        if (jumpAnimation != null) {
            animations.put(SamuraiAnimationKey.JUMP, jumpAnimation);
            LOGGER.info("Jump animation primed");
        } else {
            LOGGER.error("Failed to load jump animation; jump state will fallback to idle frames.");
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
        controller.place(worldX, worldY);
        LOGGER.info("Samurai placed at (" + worldX + ", " + worldY + ")");
    }

    public void configurePhysics(float gravity, float groundY) {
        controller.setGravity(gravity);
        controller.setGroundY(groundY);
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
        controller.update(delta);
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

        if (currentState == jumpState && controller.isGrounded()) {
            ensureIdleState();
        }
    }

    public void draw(SpriteBatch batch) {
        if (currentFrame == null) {
            LOGGER.error("Samurai current frame is null; skipping draw");
            return;
        }
        Vector2 pos = controller.position();
        float width = currentFrame.getRegionWidth();
        float height = currentFrame.getRegionHeight();
        float drawX = facingRight ? pos.x : pos.x + width;
        float drawWidth = facingRight ? width : -width;
        batch.draw(currentFrame, drawX, pos.y, drawWidth, height);
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

    public Vector2 getPosition() {
        return controller.position();
    }

    public boolean isGrounded() {
        return controller.isGrounded();
    }

    public void moveRight() {
        controller.setHorizontalSpeed(DEFAULT_RUN_SPEED);
        facingRight = true;
        startRunState();
    }

    public void moveLeft() {
        controller.setHorizontalSpeed(-DEFAULT_RUN_SPEED);
        facingRight = false;
        startRunState();
    }

    public void stopHorizontalMovement() {
        controller.stopHorizontal();
        stopRunState();
    }

    public float defaultRunSpeed() {
        return DEFAULT_RUN_SPEED;
    }

    public void aimUp() {
        if (verticalIntent != 1) {
            LOGGER.info("Vertical intent set to UP");
        }
        verticalIntent = 1;
    }

    public void aimDown() {
        if (verticalIntent != -1) {
            LOGGER.info("Vertical intent set to DOWN");
        }
        verticalIntent = -1;
    }

    public void clearVerticalIntent() {
        if (verticalIntent != 0) {
            LOGGER.info("Vertical intent reset to NEUTRAL");
        }
        verticalIntent = 0;
    }

    public int getVerticalIntent() {
        return verticalIntent;
    }

    public boolean jump() {
        if (!controller.isGrounded()) {
            return false;
        }
        controller.setVerticalVelocity(jumpSpeed);
        switchState(jumpState);
        return true;
    }

    public void setJumpSpeed(float speed) {
        this.jumpSpeed = speed;
        LOGGER.info("Jump speed updated to " + speed);
    }

    public float getJumpSpeed() {
        return jumpSpeed;
    }

    private void startRunState() {
        if (currentState != runState) {
            switchState(runState);
        }
    }

    private void stopRunState() {
        ensureIdleState();
    }
}

