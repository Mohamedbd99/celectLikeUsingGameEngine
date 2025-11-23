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
        // redDeon is the only active enemy type; other experimental enemies removed.
        String redDeonBase = "assets/emenies/redDeon/";
        int redDeonWidth = 79;
        int redDeonHeight = 69;
        register(EnemyDefinition.builder("redDeon", redDeonBase)
                .stats(new EnemyStats(150, 60f))
                .animation(EnemyAnimationKey.IDLE,
                        EnemyAnimationSpec.looping("IDLE.png", redDeonWidth, redDeonHeight, 0.1f))
                .animation(EnemyAnimationKey.HURT,
                        EnemyAnimationSpec.once("HURT.png", redDeonWidth, redDeonHeight, 0.08f))
                .animation(EnemyAnimationKey.ATTACK,
                        EnemyAnimationSpec.looping("ATTACK.png", redDeonWidth, redDeonHeight, 0.08f))
                .animation(EnemyAnimationKey.DEATH,
                        EnemyAnimationSpec.once("DEATH.png", redDeonWidth, redDeonHeight, 0.12f))
                .animation(EnemyAnimationKey.FLY,
                        EnemyAnimationSpec.looping("FLYING.png", redDeonWidth, redDeonHeight, 0.08f))
                .build());
    }
}


