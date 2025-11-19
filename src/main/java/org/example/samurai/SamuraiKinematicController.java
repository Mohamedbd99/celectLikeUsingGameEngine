package org.example.samurai;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Logger;

/**
 * Lightweight kinematic controller tailored for a Celeste-like feel.
 * Handles gravity and simple ground clamping while leaving collision logic to callers.
 */
public final class SamuraiKinematicController {

    private static final Logger LOGGER = new Logger("SamuraiController", Logger.INFO);

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();

    private float gravity = -1500f;
    private float groundY = 0f;
    private boolean grounded = false;

    public void place(float x, float y) {
        position.set(x, y);
        velocity.setZero();
        grounded = false;
        LOGGER.info("Controller placed at (" + x + ", " + y + ")");
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
        LOGGER.info("Gravity updated to " + gravity + " units/sec^2");
    }

    public void setGroundY(float groundY) {
        this.groundY = groundY;
        LOGGER.info("Ground plane set to y=" + groundY);
    }

    public void addImpulse(float impulseX, float impulseY) {
        velocity.add(impulseX, impulseY);
        LOGGER.info("Impulse applied (" + impulseX + ", " + impulseY + ")");
        if (impulseY > 0f) {
            grounded = false;
        }
    }

    public void setHorizontalSpeed(float speed) {
        if (!MathUtils.isEqual(velocity.x, speed, 0.1f)) {
            LOGGER.info("Horizontal speed set to " + speed);
        }
        velocity.x = speed;
    }

    public void stopHorizontal() {
        if (!MathUtils.isZero(velocity.x, 0.1f)) {
            LOGGER.info("Horizontal movement halted");
        }
        velocity.x = 0f;
    }

    public void setVerticalVelocity(float speed) {
        LOGGER.info("Vertical velocity set to " + speed);
        velocity.y = speed;
    }

    public void update(float delta) {
        velocity.y += gravity * delta;
        position.mulAdd(velocity, delta);

        if (position.y < groundY) {
            position.y = groundY;
            if (!grounded) {
                LOGGER.info("Samurai grounded at y=" + groundY);
            }
            grounded = true;
            if (velocity.y < 0f) {
                velocity.y = 0f;
            }
        } else {
            grounded = false;
        }
    }

    public Vector2 position() {
        return position;
    }

    public Vector2 velocity() {
        return velocity;
    }

    public boolean isGrounded() {
        return grounded;
    }
}

