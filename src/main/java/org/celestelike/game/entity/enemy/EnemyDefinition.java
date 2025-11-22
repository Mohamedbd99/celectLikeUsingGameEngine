package org.celestelike.game.entity.enemy;

import java.util.EnumMap;
import java.util.Map;

public final class EnemyDefinition {

    private final String id;
    private final String assetRoot;
    private final EnemyStats stats;
    private final EnumMap<EnemyAnimationKey, EnemyAnimationSpec> animationSpecs;

    private EnemyDefinition(Builder builder) {
        this.id = builder.id;
        this.assetRoot = builder.assetRoot;
        this.stats = builder.stats;
        this.animationSpecs = new EnumMap<>(builder.animationSpecs);
    }

    public String id() {
        return id;
    }

    public String assetRoot() {
        return assetRoot;
    }

    public EnemyStats stats() {
        return stats;
    }

    public Map<EnemyAnimationKey, EnemyAnimationSpec> animationSpecs() {
        return animationSpecs;
    }

    public static Builder builder(String id, String assetRoot) {
        return new Builder(id, assetRoot);
    }

    public static final class Builder {
        private final String id;
        private final String assetRoot;
        private EnemyStats stats = new EnemyStats(50, 48f);
        private final EnumMap<EnemyAnimationKey, EnemyAnimationSpec> animationSpecs =
                new EnumMap<>(EnemyAnimationKey.class);

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

        public EnemyDefinition build() {
            if (!animationSpecs.containsKey(EnemyAnimationKey.IDLE)) {
                throw new IllegalStateException("Enemy definition " + id + " must include an IDLE animation.");
            }
            return new EnemyDefinition(this);
        }
    }
}


