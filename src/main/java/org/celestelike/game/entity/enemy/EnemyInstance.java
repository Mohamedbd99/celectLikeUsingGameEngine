package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.celestelike.game.logging.GameLogger;

final class EnemyInstance {

    private static final Logger LOGGER = new Logger("EnemyInstance", Logger.INFO);

    private final EnemyDefinition definition;
    private final Vector2 position = new Vector2();
    private final EnumMap<EnemyAnimationKey, Animation<TextureRegion>> animations =
            new EnumMap<>(EnemyAnimationKey.class);
    private final Map<EnemyAnimationKey, EnemyAnimationSpec> specs;
    private final List<Texture> ownedTextures = new ArrayList<>();
    private final EnemyStats stats;
    private final float width;
    private final float height;

    private EnemyAnimationKey currentKey = EnemyAnimationKey.IDLE;
    private float stateTime;
    private float hurtTimer;
    private int health;
    private boolean dead;
    
    // Movement and attack state
    private Vector2 playerPosition = null;
    private float attackCooldownTimer = 0f;
    private boolean isAttacking = false;
    private float attackStartTime = 0f;
    private boolean facingRight = true; // Direction enemy is facing

    EnemyInstance(EnemyDefinition definition, float x, float y) {
        this.definition = definition;
        this.stats = definition.stats();
        this.health = stats.maxHealth();
        this.position.set(x, y);
        this.specs = new EnumMap<>(definition.animationSpecs());
        loadAnimations(definition.assetRoot(), specs);
        Animation<TextureRegion> idle = animations.get(EnemyAnimationKey.IDLE);
        if (idle == null) {
            throw new IllegalStateException("Enemy " + definition.id() + " missing idle animation");
        }
        TextureRegion firstFrame = idle.getKeyFrame(0f);
        this.width = firstFrame.getRegionWidth();
        this.height = firstFrame.getRegionHeight();
        GameLogger.entityCreated("Enemy", definition.id());
    }

    private void loadAnimations(String root, Map<EnemyAnimationKey, EnemyAnimationSpec> specs) {
        specs.forEach((key, spec) -> {
            Animation<TextureRegion> animation = buildAnimation(root + spec.file(), spec);
            if (animation != null) {
                animations.put(key, animation);
            }
        });
    }

    private Animation<TextureRegion> buildAnimation(String path, EnemyAnimationSpec spec) {
        try {
            Texture texture = new Texture(Gdx.files.internal(path));
            ownedTextures.add(texture);
            int frameWidth = spec.frameWidth() > 0 ? spec.frameWidth() : texture.getWidth();
            int frameHeight = spec.frameHeight() > 0 ? spec.frameHeight() : texture.getHeight();
            frameWidth = Math.max(1, Math.min(frameWidth, texture.getWidth()));
            frameHeight = Math.max(1, Math.min(frameHeight, texture.getHeight()));
            TextureRegion[][] split = TextureRegion.split(texture, frameWidth, frameHeight);
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
                LOGGER.error("Spritesheet " + path + " produced no frames");
                return null;
            }
            return new Animation<>(spec.frameDuration(), frames, spec.playMode());
        } catch (Exception exception) {
            LOGGER.error("Failed to load animation " + path, exception);
            return null;
        }
    }

    void update(float delta, Vector2 playerPos) {
        this.playerPosition = playerPos;
        
        if (dead) {
            Animation<TextureRegion> death = animations.get(EnemyAnimationKey.DEATH);
            if (death != null && death.isAnimationFinished(stateTime)) {
                // stay on last frame
                stateTime = death.getAnimationDuration();
            } else {
                stateTime += delta;
            }
            return;
        }
        
        stateTime += delta;
        
        // Handle hurt state
        if (hurtTimer > 0f) {
            hurtTimer -= delta;
            if (hurtTimer <= 0f && !dead) {
                changeAnimation(EnemyAnimationKey.IDLE);
            }
            return; // Don't move or attack while hurt
        }
        
        // Handle attack state
        if (isAttacking) {
            Animation<TextureRegion> attackAnim = animations.get(EnemyAnimationKey.ATTACK);
            float attackElapsed = stateTime - attackStartTime;
            float attackDuration = attackAnim != null ? attackAnim.getAnimationDuration() : 0.5f;
            
            // For looping animations, use a fixed duration
            if (attackAnim != null && attackAnim.getPlayMode() == Animation.PlayMode.LOOP) {
                attackDuration = 0.6f; // Fixed duration for looping attacks
            }
            
    // Wait for animation to fully complete before allowing next attack
    if (attackAnim != null && attackAnim.getPlayMode() != Animation.PlayMode.LOOP) {
        // For non-looping animations, wait until animation is finished
        if (attackAnim.isAnimationFinished(attackElapsed)) {
            isAttacking = false;
            attackCooldownTimer = stats.attackCooldown();
            changeAnimation(EnemyAnimationKey.IDLE);
        }
    } else {
        // For looping animations, use fixed duration
        if (attackElapsed >= attackDuration) {
            isAttacking = false;
            attackCooldownTimer = stats.attackCooldown();
            changeAnimation(EnemyAnimationKey.IDLE);
        }
    }
    return;        }
        
        // Update attack cooldown
        if (attackCooldownTimer > 0f) {
            attackCooldownTimer -= delta;
        }
        
        // Check if player is in range and attack (only if not already attacking)
        if (playerPosition != null && !isAttacking && attackCooldownTimer <= 0f) {
            float centerX = position.x + width * 0.5f;
            float centerY = position.y + height * 0.5f;
            float distance = playerPosition.dst(centerX, centerY);
            
            // Check attack range using horizontal distance only (not vertical)
            float horizontalDistance = Math.abs(playerPosition.x - centerX);
            if (horizontalDistance <= stats.attackRange()) {
                // Update facing direction before attacking (inverted - face towards player)
                facingRight = playerPosition.x < centerX;
                
                // Start attack
                isAttacking = true;
                attackStartTime = stateTime;
                changeAnimation(EnemyAnimationKey.ATTACK);
                return;
            }
        }
        
        // Move towards player if not attacking and player exists (horizontal only)
        if (playerPosition != null && !isAttacking) {
            float centerX = position.x + width * 0.5f;
            float centerY = position.y + height * 0.5f;
            float distanceX = Math.abs(playerPosition.x - centerX);
            float distanceY = Math.abs(playerPosition.y - centerY);
            float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
            
            // Update facing direction based on player position (inverted - face towards player)
            facingRight = playerPosition.x < centerX;
            
            // Only move if player is within detection range (slightly larger than attack range)
            float detectionRange = stats.attackRange() * 2f;
            if (distance > stats.attackRange() && distance <= detectionRange) {
                // Calculate horizontal direction to player (only left/right)
                float dirX = playerPosition.x - centerX;
                
                if (Math.abs(dirX) > 5f) {
                    // Normalize horizontal direction
                    dirX = dirX > 0 ? 1f : -1f;
                    
                    // Move horizontally only
                    float moveSpeed = stats.moveSpeed();
                    position.x += dirX * moveSpeed * delta;
                    // Don't change Y position - only horizontal movement
                    
                    // Use FLY animation for movement (walking)
                    if (currentKey != EnemyAnimationKey.FLY) {
                        changeAnimation(EnemyAnimationKey.FLY);
                    }
                } else {
                    // Very close horizontally, use idle
                    if (currentKey != EnemyAnimationKey.IDLE) {
                        changeAnimation(EnemyAnimationKey.IDLE);
                    }
                }
            } else {
                // Player too far or too close, use idle
                if (currentKey != EnemyAnimationKey.IDLE) {
                    changeAnimation(EnemyAnimationKey.IDLE);
                }
            }
        } else {
            // No player position, use idle
            if (currentKey != EnemyAnimationKey.IDLE) {
                changeAnimation(EnemyAnimationKey.IDLE);
            }
        }
    }
    
    void update(float delta) {
        update(delta, null);
    }

    void draw(SpriteBatch batch) {
        Animation<TextureRegion> animation = animations.get(currentKey);
        if (animation == null) {
            return;
        }
        EnemyAnimationSpec spec = specs.get(currentKey);
        TextureRegion frame = animation.getKeyFrame(stateTime, animation.getPlayMode() == Animation.PlayMode.LOOP);
        float frameWidth = frame.getRegionWidth();
        float frameHeight = frame.getRegionHeight();
        
        float offsetX = spec != null ? spec.offsetX() : 0f;
        float offsetY = spec != null ? spec.offsetY() : 0f;
        
        // Flip sprite horizontally based on facing direction
        float drawWidth = facingRight ? frameWidth : -frameWidth;
        float drawX = facingRight ? position.x + offsetX : position.x + frameWidth - offsetX;
        float drawY = position.y + offsetY;
        
        batch.draw(frame, drawX, drawY, drawWidth, frameHeight);
    }

    void drawHealthBarFill(ShapeRenderer shapeRenderer) {
        if (dead) {
            return;
        }
        float barWidth = width;
        float barHeight = 6f;
        float x = position.x;
        float y = position.y + height + 6f;
        float ratio = Math.max(0f, health / (float) stats.maxHealth());

        shapeRenderer.setColor(0f, 0f, 0f, 0.65f);
        shapeRenderer.rect(x - 1f, y - 1f, barWidth + 2f, barHeight + 2f);
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.85f);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.setColor(0.95f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(x, y, barWidth * ratio, barHeight);
    }

    void drawHealthBarOutline(ShapeRenderer shapeRenderer) {
        if (dead) {
            return;
        }
        float barWidth = width;
        float barHeight = 6f;
        float x = position.x;
        float y = position.y + height + 6f;
        shapeRenderer.setColor(0.05f, 0.05f, 0.05f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        int segments = Math.max(1, (int) Math.ceil(stats.maxHealth() / 10f));
        float step = barWidth / segments;
        for (int i = 1; i < segments; i++) {
            float tickX = x + step * i;
            shapeRenderer.line(tickX, y, tickX, y + barHeight);
        }
    }

    boolean applyDamage(int amount) {
        if (dead) {
            return false;
        }
        health = Math.max(0, health - amount);
        GameLogger.info(definition.id() + " took " + amount + " dmg (hp=" + health + "/" + stats.maxHealth() + ")");
        if (health == 0) {
            dead = true;
            changeAnimation(EnemyAnimationKey.DEATH);
            return true;
        } else {
            hurtTimer = 0.3f;
            changeAnimation(EnemyAnimationKey.HURT);
            return false;
        }
    }

    boolean isDead() {
        if (!dead) {
            return false;
        }
        Animation<TextureRegion> death = animations.get(EnemyAnimationKey.DEATH);
        if (death == null) {
            // No death animation configured: despawn immediately.
            return true;
        }
        // Only report "dead" to the manager once the death animation has finished.
        return death.isAnimationFinished(stateTime);
    }

    int maxHealth() {
        return stats.maxHealth();
    }

    int currentHealth() {
        return health;
    }

    Vector2 position() {
        return position;
    }

    float width() {
        return width;
    }

    float height() {
        return height;
    }

    float radius() {
        return stats.contactRadius();
    }

    void dispose() {
        for (Texture texture : ownedTextures) {
            if (texture != null) {
                texture.dispose();
            }
        }
        ownedTextures.clear();
        GameLogger.entityDestroyed("Enemy", definition.id());
    }

    EnemyDefinition definition() {
        return definition;
    }
    
    boolean isAttacking() {
        return isAttacking;
    }
    
    boolean canDealDamage() {
        if (!isAttacking || playerPosition == null) {
            return false;
        }
        Animation<TextureRegion> attackAnim = animations.get(EnemyAnimationKey.ATTACK);
        if (attackAnim == null) {
            return false;
        }
        float attackElapsed = stateTime - attackStartTime;
        float attackDuration = attackAnim.getAnimationDuration();
        
        // For looping animations, use a fixed duration
        if (attackAnim.getPlayMode() == Animation.PlayMode.LOOP) {
            attackDuration = 0.6f;
        }
        
        // Deal damage at the middle of the attack animation
        float attackProgress = attackDuration > 0 ? attackElapsed / attackDuration : 0f;
        return attackProgress >= 0.3f && attackProgress <= 0.7f;
    }
    
    int getAttackDamage() {
        return stats.attackDamage();
    }

    private void changeAnimation(EnemyAnimationKey key) {
        if (animations.containsKey(key)) {
            currentKey = key;
            stateTime = 0f;
        }
    }
}


