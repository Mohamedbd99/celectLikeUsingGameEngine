package org.celestelike.game.entity.samurai;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Logger;
import org.celestelike.game.world.LevelCollisionMap;

/**
 * Lightweight kinematic controller tailored for a Celeste-like feel.
 * Handles gravity and simple ground clamping while leaving collision logic to callers.
 */
public final class SamuraiKinematicController {

    private static final Logger LOGGER = new Logger("SamuraiController", Logger.INFO);
    private static final float COLLISION_EPSILON = 0.001f;

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();

    private float gravity = -1500f;
    private float groundY = 0f;
    private boolean grounded = false;
    private LevelCollisionMap collisionMap;
    private boolean touchingWallLeft;
    private boolean touchingWallRight;
    private boolean inWater;
    private float colliderWidth = 48f;
    private float colliderHeight = 80f;
    private float colliderOffsetX = 2f;
    private float colliderOffsetY = 0f;

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

    public void setCollisionMap(LevelCollisionMap collisionMap) {
        this.collisionMap = collisionMap;
    }

    public void configureCollider(float width, float height, float offsetX, float offsetY) {
        this.colliderWidth = width;
        this.colliderHeight = height;
        this.colliderOffsetX = offsetX;
        this.colliderOffsetY = offsetY;
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
        if (collisionMap == null) {
            touchingWallLeft = false;
            touchingWallRight = false;
            inWater = false;
            legacyIntegrate(delta);
        } else {
            integrateWithCollisions(delta);
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

    public boolean isTouchingWallLeft() {
        return touchingWallLeft;
    }

    public boolean isTouchingWallRight() {
        return touchingWallRight;
    }

    public boolean isInWater() {
        return inWater;
    }

    private void refreshWallTouchFlags() {
        if (collisionMap == null) {
            touchingWallLeft = false;
            touchingWallRight = false;
            return;
        }
        float bottom = colliderBottom() + COLLISION_EPSILON;
        float top = colliderTop() - COLLISION_EPSILON;
        int rowBottom = collisionMap.worldToRow(bottom);
        int rowTop = collisionMap.worldToRow(top);
        int rowStart = Math.min(rowBottom, rowTop);
        int rowEnd = Math.max(rowBottom, rowTop);

        boolean leftContact = false;
        boolean rightContact = false;

        float probeLeft = colliderLeft() - COLLISION_EPSILON;
        int colLeft = collisionMap.worldToCol(probeLeft);
        for (int row = rowStart; row <= rowEnd; row++) {
            if (collisionMap.isSolid(row, colLeft)) {
                leftContact = true;
                break;
            }
        }

        float probeRight = colliderRight() + COLLISION_EPSILON;
        int colRight = collisionMap.worldToCol(probeRight);
        for (int row = rowStart; row <= rowEnd; row++) {
            if (collisionMap.isSolid(row, colRight)) {
                rightContact = true;
                break;
            }
        }

        touchingWallLeft = leftContact;
        touchingWallRight = rightContact;
    }

    private void refreshWaterFlag() {
        if (collisionMap == null) {
            inWater = false;
            return;
        }
        float left = colliderLeft() + COLLISION_EPSILON;
        float right = colliderRight() - COLLISION_EPSILON;
        float bottom = colliderBottom() + COLLISION_EPSILON;
        float top = colliderTop() - COLLISION_EPSILON;
        inWater = collisionMap.overlapsWater(left, bottom, right, top);
    }

    private void legacyIntegrate(float delta) {
        touchingWallLeft = false;
        touchingWallRight = false;
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
        inWater = false;
    }

    private void integrateWithCollisions(float delta) {
        touchingWallLeft = false;
        touchingWallRight = false;
        position.x += velocity.x * delta;
        resolveHorizontalCollision();

        position.y += velocity.y * delta;
        resolveVerticalCollision();
        refreshWallTouchFlags();
        refreshWaterFlag();
    }

    private void resolveHorizontalCollision() {
        if (MathUtils.isZero(velocity.x, 0.01f)) {
            return;
        }

        float bottom = colliderBottom() + COLLISION_EPSILON;
        float top = colliderTop() - COLLISION_EPSILON;
        int rowBottom = collisionMap.worldToRow(bottom);
        int rowTop = collisionMap.worldToRow(top);
        int rowStart = Math.min(rowBottom, rowTop);
        int rowEnd = Math.max(rowBottom, rowTop);

        if (velocity.x > 0f) {
            float probeX = colliderRight() - COLLISION_EPSILON;
            int col = collisionMap.worldToCol(probeX);
            for (int row = rowStart; row <= rowEnd; row++) {
                if (collisionMap.isSolid(row, col)) {
                    float tileLeft = collisionMap.colLeft(col);
                    float newLeft = tileLeft - colliderWidth;
                    setColliderLeft(newLeft);
                    velocity.x = 0f;
                    touchingWallRight = true;
                    break;
    }
            }
        } else {
            float probeX = colliderLeft() + COLLISION_EPSILON;
            int col = collisionMap.worldToCol(probeX);
            for (int row = rowStart; row <= rowEnd; row++) {
                if (collisionMap.isSolid(row, col)) {
                    float tileRight = collisionMap.colLeft(col) + collisionMap.tileSize();
                    float newLeft = tileRight;
                    setColliderLeft(newLeft);
                    velocity.x = 0f;
                    touchingWallLeft = true;
                    break;
                }
            }
        }
    }

    private void resolveVerticalCollision() {
        float left = colliderLeft() + COLLISION_EPSILON;
        float right = colliderRight() - COLLISION_EPSILON;
        int colLeft = collisionMap.worldToCol(left);
        int colRight = collisionMap.worldToCol(right);
        int colStart = Math.min(colLeft, colRight);
        int colEnd = Math.max(colLeft, colRight);

        if (velocity.y <= 0f) {
            float probeY = colliderBottom() - COLLISION_EPSILON;
            int row = collisionMap.worldToRow(probeY);
            boolean collided = false;
            for (int col = colStart; col <= colEnd; col++) {
                if (collisionMap.isSolid(row, col)) {
                    float tileTop = collisionMap.rowTop(row);
                    setColliderBottom(tileTop);
                    velocity.y = 0f;
                    grounded = true;
                    collided = true;
                    break;
                }
            }
            if (!collided) {
                grounded = false;
            }
        } else {
            float probeY = colliderTop() + COLLISION_EPSILON;
            int row = collisionMap.worldToRow(probeY);
            for (int col = colStart; col <= colEnd; col++) {
                if (collisionMap.isSolid(row, col)) {
                    float tileBottom = collisionMap.rowBottom(row);
                    setColliderBottom(tileBottom - colliderHeight);
                    velocity.y = 0f;
                    break;
                }
            }
            grounded = false;
        }
    }

    private float colliderLeft() {
        return position.x + colliderOffsetX;
    }

    private float colliderRight() {
        return colliderLeft() + colliderWidth;
    }

    private float colliderBottom() {
        return position.y + colliderOffsetY;
    }

    private float colliderTop() {
        return colliderBottom() + colliderHeight;
    }

    private void setColliderLeft(float newLeft) {
        position.x = newLeft - colliderOffsetX;
    }

    private void setColliderBottom(float newBottom) {
        position.y = newBottom - colliderOffsetY;
    }
}

