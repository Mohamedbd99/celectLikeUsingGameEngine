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

    void update(float delta) {
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
        if (hurtTimer > 0f) {
            hurtTimer -= delta;
            if (hurtTimer <= 0f && !dead) {
                changeAnimation(EnemyAnimationKey.IDLE);
            }
        }
    }

    void draw(SpriteBatch batch) {
        Animation<TextureRegion> animation = animations.get(currentKey);
        if (animation == null) {
            return;
        }
        EnemyAnimationSpec spec = specs.get(currentKey);
        TextureRegion frame = animation.getKeyFrame(stateTime, animation.getPlayMode() == Animation.PlayMode.LOOP);
        float drawX = position.x;
        float drawY = position.y;
        if (spec != null) {
            drawX += spec.offsetX();
            drawY += spec.offsetY();
        }
        batch.draw(frame, drawX, drawY);
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

    void applyDamage(int amount) {
        if (dead) {
            return;
        }
        health = Math.max(0, health - amount);
        GameLogger.info(definition.id() + " took " + amount + " dmg (hp=" + health + "/" + stats.maxHealth() + ")");
        if (health == 0) {
            dead = true;
            changeAnimation(EnemyAnimationKey.DEATH);
        } else {
            hurtTimer = 0.3f;
            changeAnimation(EnemyAnimationKey.HURT);
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

    private void changeAnimation(EnemyAnimationKey key) {
        if (animations.containsKey(key)) {
            currentKey = key;
            stateTime = 0f;
        }
    }
}


