package org.celestelike.game.entity.enemy;

import java.util.EnumMap;
import java.util.Map;

public final class EnemyDefinition {

    private final String id;
    private final String assetRoot;
    private final EnemyStats stats;
    private final EnumMap<EnemyAnimationKey, EnemyAnimationSpec> animationSpecs;

    // ✅ NEW: rendering tuning
    private final float renderScale;
    private final float renderOriginX;
    private final float renderOriginY;

    private EnemyDefinition(Builder builder) {
        this.id = builder.id;
        this.assetRoot = builder.assetRoot;
        this.stats = builder.stats;
        this.animationSpecs = new EnumMap<>(builder.animationSpecs);

        this.renderScale = builder.renderScale;
        this.renderOriginX = builder.renderOriginX;
        this.renderOriginY = builder.renderOriginY;
    }

    public String id() { return id; }
    public String assetRoot() { return assetRoot; }
    public EnemyStats stats() { return stats; }
    public Map<EnemyAnimationKey, EnemyAnimationSpec> animationSpecs() { return animationSpecs; }

    // ✅ NEW getters
    public float renderScale() { return renderScale; }
    public float renderOriginX() { return renderOriginX; }
    public float renderOriginY() { return renderOriginY; }

    public static Builder builder(String id, String assetRoot) {
        return new Builder(id, assetRoot);
    }

    public static final class Builder {
        private final String id;
        private final String assetRoot;

        private EnemyStats stats = new EnemyStats(50, 48f);
        private final EnumMap<EnemyAnimationKey, EnemyAnimationSpec> animationSpecs =
                new EnumMap<>(EnemyAnimationKey.class);

        // ✅ defaults safe for "normal" sprites
        private float renderScale = 1f;
        private float renderOriginX = 0f; // default: bottom-left like batch.draw(x,y)
        private float renderOriginY = 0f;

        private Builder(String id, String assetRoot) {
            this.id = id;
            this.assetRoot = assetRoot;
        }

        public Builder stats(EnemyStats stats) {
            this.stats = stats;
            return this;
        }

        public Builder animation(EnemyAnimationKey key, EnemyAnimationSpec spec) {
            animationSpecs.put(key, spec);
            return this;
        }

        // ✅ NEW builder setters
        public Builder renderScale(float scale) {
            this.renderScale = scale;
            return this;
        }

        /** origin in pixels inside the frame, measured from bottom-left */
        public Builder renderOrigin(float originX, float originY) {
            this.renderOriginX = originX;
            this.renderOriginY = originY;
            return this;
        }

        public EnemyDefinition build() {
            if (!animationSpecs.containsKey(EnemyAnimationKey.IDLE)) {
                throw new IllegalStateException("Enemy definition " + id + " must include an IDLE animation.");
            }
            return new EnemyDefinition(this);
        }
    }
}
