package org.celestelike.game.entity.samurai;

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
import org.celestelike.game.entity.samurai.state.SamuraiDashState;
import org.celestelike.game.entity.samurai.state.SamuraiIdleState;
import org.celestelike.game.entity.samurai.state.SamuraiJumpState;
import org.celestelike.game.entity.samurai.state.SamuraiRunState;
import org.celestelike.game.entity.samurai.state.SamuraiState;
import org.celestelike.game.world.LevelCollisionMap;

/**
 * Samurai hero facade. Handles loading, state updates, animation selection, and rendering.
 */
public final class SamuraiCharacter {

    private static final Logger LOGGER = new Logger("SamuraiCharacter", Logger.INFO);
    private static final String SAMURAI_BASE = "assets/FULL_Samurai 2D Pixel Art v1.2/Sprites/";
    private static final String IDLE_FILE = SAMURAI_BASE + "IDLE.png";
    private static final String RUN_FILE = SAMURAI_BASE + "RUN.png";
    private static final String JUMP_START_FILE = SAMURAI_BASE + "JUMP-START.png";
    private static final String JUMP_TRANSITION_FILE = SAMURAI_BASE + "JUMP-TRANSITION.png";
    private static final String JUMP_FALL_FILE = SAMURAI_BASE + "JUMP-FALL.png";
    private static final String DASH_FILE = SAMURAI_BASE + "DASH.png";
    private static final int FRAME_SIZE = 96;
    private static final float DEFAULT_FRAME_DURATION = 0.08f;
    private static final float RUN_FRAME_DURATION = 0.05f;
    private static final float JUMP_START_DURATION = 0.05f;
    private static final float JUMP_TRANSITION_DURATION = 0.05f;
    private static final float JUMP_FALL_FRAME_DURATION = 0.06f;
    private static final float DASH_FRAME_DURATION = 0.04f;
    private static final float DEFAULT_RUN_SPEED = 220f;
    private static final float DEFAULT_JUMP_SPEED = 620f;
    private static final float DASH_SPEED = 700f;
    private static final float DASH_DURATION = 0.25f;
    private static final int DASH_MAX_FRAMES = 5;
    private static final float RENDER_OFFSET_Y = -20f;
    private static final float RENDER_OFFSET_X = -10f;
    private final Vector2 dashDirection = new Vector2();

    private final EnumMap<SamuraiAnimationKey, Animation<TextureRegion>> animations =
            new EnumMap<>(SamuraiAnimationKey.class);
    private final List<Texture> ownedTextures = new ArrayList<>();
    private final SamuraiIdleState idleState = new SamuraiIdleState();
    private final SamuraiRunState runState = new SamuraiRunState();
    private final SamuraiJumpState jumpState = new SamuraiJumpState();
    private final SamuraiDashState dashState = new SamuraiDashState();
    private final SamuraiKinematicController controller = new SamuraiKinematicController();

    private SamuraiState currentState;
    private TextureRegion currentFrame;
    private float stateTime;
    private int verticalIntent; // -1 down, 0 neutral, 1 up
    private boolean facingRight = true;
    private float jumpSpeed = DEFAULT_JUMP_SPEED;
    private JumpPhase jumpPhase = JumpPhase.START;
    private float jumpPhaseTime;
    private float jumpStartDuration;
    private float jumpTransitionDuration;
    private boolean dashAvailable = true;
    private boolean jumpAvailable = true;
    private boolean isDashing;
    private float dashTimer;
    private boolean wasGrounded;

    public SamuraiCharacter() {
        LOGGER.info("Samurai character initialized");
        controller.configureCollider(40f, 84f, 50f, 0f);
        dashAvailable = true;
        jumpAvailable = true;
        wasGrounded = false;
    }

    public void loadAssets() {
        LOGGER.info("Loading samurai spritesheets");
        Animation<TextureRegion> idleAnimation = loadAnimation(IDLE_FILE, DEFAULT_FRAME_DURATION, Animation.PlayMode.LOOP);
        Animation<TextureRegion> runAnimation = loadAnimation(RUN_FILE, RUN_FRAME_DURATION, Animation.PlayMode.LOOP);
        Animation<TextureRegion> jumpStartAnimation = loadAnimation(JUMP_START_FILE, JUMP_START_DURATION, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> jumpTransitionAnimation = loadAnimation(JUMP_TRANSITION_FILE, JUMP_TRANSITION_DURATION, Animation.PlayMode.NORMAL);
        Animation<TextureRegion> jumpFallAnimation = loadAnimation(JUMP_FALL_FILE, JUMP_FALL_FRAME_DURATION, Animation.PlayMode.NORMAL);

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

        if (jumpStartAnimation != null) {
            animations.put(SamuraiAnimationKey.JUMP, jumpStartAnimation);
            jumpStartDuration = jumpStartAnimation.getAnimationDuration();
            LOGGER.info("Jump start animation primed");
        } else {
            LOGGER.error("Failed to load jump start animation.");
        }

        if (jumpTransitionAnimation != null) {
            animations.put(SamuraiAnimationKey.JUMP_TRANSITION, jumpTransitionAnimation);
            jumpTransitionDuration = jumpTransitionAnimation.getAnimationDuration();
            LOGGER.info("Jump transition animation primed");
        } else {
            LOGGER.error("Failed to load jump transition animation.");
        }

        if (jumpFallAnimation != null) {
            animations.put(SamuraiAnimationKey.JUMP_FALL, jumpFallAnimation);
            LOGGER.info("Jump fall animation primed");
        } else {
            LOGGER.error("Failed to load jump fall animation.");
        }

        Animation<TextureRegion> dashAnimation = loadAnimation(DASH_FILE, DASH_FRAME_DURATION, Animation.PlayMode.LOOP, DASH_MAX_FRAMES);
        if (dashAnimation != null) {
            animations.put(SamuraiAnimationKey.DASH, dashAnimation);
            LOGGER.info("Dash animation primed");
        } else {
            LOGGER.error("Failed to load dash animation.");
        }
    }

    private Animation<TextureRegion> loadAnimation(String path, float frameDuration, Animation.PlayMode playMode) {
        return loadAnimation(path, frameDuration, playMode, Integer.MAX_VALUE);
    }

    private Animation<TextureRegion> loadAnimation(String path, float frameDuration, Animation.PlayMode playMode, int maxFrames) {
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            LOGGER.error("Missing animation sheet: " + path);
            return null;
        }
        Texture texture = new Texture(handle);
        ownedTextures.add(texture);
        TextureRegion[][] split = TextureRegion.split(texture, FRAME_SIZE, FRAME_SIZE);
        Array<TextureRegion> frames = new Array<>();
        int added = 0;
        outer:
        for (TextureRegion[] row : split) {
            for (TextureRegion region : row) {
                if (region == null) {
                    continue;
                }
                frames.add(region);
                added++;
                if (added >= maxFrames) {
                    break outer;
                }
            }
        }
        if (frames.isEmpty()) {
            LOGGER.error("Spritesheet had no frames: " + path);
            return null;
        }
        LOGGER.info("Loaded " + frames.size + " frames from " + path);
        return new Animation<>(frameDuration, frames, playMode);
    }

    public void placeAt(float worldX, float worldY) {
        controller.place(worldX, worldY);
        LOGGER.info("Samurai placed at (" + worldX + ", " + worldY + ")");
    }

    public void configurePhysics(float gravity, float groundY) {
        controller.setGravity(gravity);
        controller.setGroundY(groundY);
    }

    public void attachCollisionMap(LevelCollisionMap collisionMap) {
        controller.setCollisionMap(collisionMap);
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
        tickDash(delta);
        handleGrounding(controller.isGrounded());
        if (currentState == null) {
            LOGGER.error("Samurai has no active state; invoking ensureIdleState");
            ensureIdleState();
        }
        if (currentState != null) {
            currentState.update(this, delta);
            updateJumpPhase(delta);
            Animation<TextureRegion> animation = animations.get(resolveAnimationKey());
            if (animation != null) {
                float animationTime = currentState == jumpState ? jumpPhaseTime : stateTime;
                boolean loop = animation.getPlayMode() == Animation.PlayMode.LOOP || animation.getPlayMode() == Animation.PlayMode.LOOP_REVERSED;
                currentFrame = animation.getKeyFrame(animationTime, loop);
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
        Vector2 pos = controller.position();
        float width = currentFrame.getRegionWidth();
        float height = currentFrame.getRegionHeight();
        float drawWidth = facingRight ? width : -width;
        float drawX = facingRight ? pos.x + RENDER_OFFSET_X : pos.x + width - RENDER_OFFSET_X;
        float drawY = pos.y + RENDER_OFFSET_Y;
        batch.draw(currentFrame, drawX, drawY, drawWidth, height);
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
        if (isDashing) {
            return;
        }
        controller.setHorizontalSpeed(DEFAULT_RUN_SPEED);
        facingRight = true;
        if (controller.isGrounded()) {
            startRunState();
        }
    }

    public void moveLeft() {
        if (isDashing) {
            return;
        }
        controller.setHorizontalSpeed(-DEFAULT_RUN_SPEED);
        facingRight = false;
        if (controller.isGrounded()) {
            startRunState();
        }
    }

    public void stopHorizontalMovement() {
        if (isDashing) {
            return;
        }
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
        if (!jumpAvailable || !controller.isGrounded()) {
            return false;
        }
        jumpAvailable = false;
        controller.setVerticalVelocity(jumpSpeed);
        jumpPhase = JumpPhase.START;
        jumpPhaseTime = 0f;
        switchState(jumpState);
        return true;
    }

    public boolean dash(float dirX, float dirY) {
        if (!dashAvailable || isDashing) {
            return false;
        }
        Vector2 direction = dashDirection.set(dirX, dirY);
        if (direction.isZero()) {
            direction.set(facingRight ? 1f : -1f, 0f);
        }
        direction.nor();
        dashAvailable = false;
        isDashing = true;
        dashTimer = DASH_DURATION;
        controller.setHorizontalSpeed(direction.x * DASH_SPEED);
        controller.setVerticalVelocity(direction.y * DASH_SPEED);
        if (direction.x > 0.01f) {
            facingRight = true;
        } else if (direction.x < -0.01f) {
            facingRight = false;
        }
        switchState(dashState);
        return true;
    }

    public void setJumpSpeed(float speed) {
        this.jumpSpeed = speed;
        LOGGER.info("Jump speed updated to " + speed);
    }

    public float getJumpSpeed() {
        return jumpSpeed;
    }

    private void tickDash(float delta) {
        if (!isDashing) {
            return;
        }
        dashTimer -= delta;
        if (dashTimer <= 0f) {
            endDash();
        }
    }

    private void endDash() {
        if (!isDashing) {
            return;
        }
        isDashing = false;
        controller.stopHorizontal();
        if (controller.isGrounded()) {
            restoreAirActions();
            ensureIdleState();
        } else {
            switchState(jumpState);
            advanceJumpPhase(JumpPhase.FALL);
        }
    }

    private void startRunState() {
        if (currentState != runState) {
            switchState(runState);
        }
    }

    private void stopRunState() {
        ensureIdleState();
    }

    private void updateJumpPhase(float delta) {
        if (currentState != jumpState) {
            return;
        }
        jumpPhaseTime += delta;
        switch (jumpPhase) {
            case START -> {
                if (jumpPhaseTime >= jumpStartDuration) {
                    advanceJumpPhase(JumpPhase.TRANSITION);
                }
            }
            case TRANSITION -> {
                if (jumpPhaseTime >= jumpTransitionDuration) {
                    advanceJumpPhase(JumpPhase.FALL);
                }
            }
            case FALL -> {
                // stay until grounded (handled elsewhere)
            }
        }
    }

    private void advanceJumpPhase(JumpPhase phase) {
        jumpPhase = phase;
        jumpPhaseTime = 0f;
    }

    private void handleGrounding(boolean grounded) {
        if (grounded && !wasGrounded) {
            restoreAirActions();
        }
        if (grounded && !isDashing && currentState == jumpState) {
            ensureIdleState();
        }
        wasGrounded = grounded;
    }

    private void restoreAirActions() {
        dashAvailable = true;
        jumpAvailable = true;
    }

    private SamuraiAnimationKey resolveAnimationKey() {
        if (currentState == jumpState) {
            return switch (jumpPhase) {
                case START -> SamuraiAnimationKey.JUMP;
                case TRANSITION -> SamuraiAnimationKey.JUMP_TRANSITION;
                case FALL -> SamuraiAnimationKey.JUMP_FALL;
            };
        }
        return currentState.animationKey();
    }

    private enum JumpPhase {
        START,
        TRANSITION,
        FALL
    }
}

