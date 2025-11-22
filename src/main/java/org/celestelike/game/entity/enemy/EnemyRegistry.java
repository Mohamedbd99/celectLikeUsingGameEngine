package org.celestelike.game.entity.enemy;

import java.util.HashMap;
import java.util.Map;

/**
 * Central lookup table for all known enemy variants.
 */
public final class EnemyRegistry {

    private static final Map<String, EnemyDefinition> DEFINITIONS = new HashMap<>();

    private EnemyRegistry() {}

    public static void register(EnemyDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    public static EnemyDefinition definition(String id) {
        return DEFINITIONS.get(id);
    }

    public static void registerDefaults() {
        if (!DEFINITIONS.isEmpty()) {
            return;
        }
        // redDeon assets (frame sizes can be tuned later)
        String base = "assets/emenies/redDeon/";
        int frameWidth = 79;
        int frameHeight = 69;
        register(EnemyDefinition.builder("redDeon", base)
                .stats(new EnemyStats(150, 60f))
                .animation(EnemyAnimationKey.IDLE, EnemyAnimationSpec.looping("IDLE.png", frameWidth, frameHeight, 0.1f))
                .animation(EnemyAnimationKey.HURT, EnemyAnimationSpec.once("HURT.png", frameWidth, frameHeight, 0.08f))
                .animation(EnemyAnimationKey.ATTACK, EnemyAnimationSpec.looping("ATTACK.png", frameWidth, frameHeight, 0.08f))
                .animation(EnemyAnimationKey.DEATH, EnemyAnimationSpec.once("DEATH.png", frameWidth, frameHeight, 0.12f))
                .animation(EnemyAnimationKey.FLY, EnemyAnimationSpec.looping("FLYING.png", frameWidth, frameHeight, 0.08f))
                .build());
    }
}


