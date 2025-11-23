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

        // deathBoss – initial simple setup (single-frame animations; refine later)
        // Sprites live under assets/emenies/deathBoss/
        String deathBossBase = "assets/emenies/deathBoss/";
        register(EnemyDefinition.builder("deathBoss", deathBossBase)
                .stats(new EnemyStats(800, 80f))
                .animation(EnemyAnimationKey.IDLE,
                        EnemyAnimationSpec.looping("IDLE.png", 0, 0, 0.18f))
                .animation(EnemyAnimationKey.ATTACK,
                        EnemyAnimationSpec.looping("attacking.png", 0, 0, 0.12f))
                .animation(EnemyAnimationKey.HURT,
                        EnemyAnimationSpec.once("idle2.png", 0, 0, 0.08f))
                .animation(EnemyAnimationKey.DEATH,
                        EnemyAnimationSpec.once("death.png", 0, 0, 0.16f))
                .build());

        // skeletonEnemie – basic idle/attack/hurt/death so snapshot entries work
        String skeletonBase = "assets/emenies/skeletonEnemie/";
        register(EnemyDefinition.builder("skeletonEnemie", skeletonBase)
                .stats(new EnemyStats(220, 50f))
                .animation(EnemyAnimationKey.IDLE,
                        EnemyAnimationSpec.looping("Idle.png", 0, 0, 0.16f))
                .animation(EnemyAnimationKey.ATTACK,
                        EnemyAnimationSpec.looping("Attack.png", 0, 0, 0.12f))
                .animation(EnemyAnimationKey.HURT,
                        EnemyAnimationSpec.once("Take Hit.png", 0, 0, 0.08f))
                .animation(EnemyAnimationKey.DEATH,
                        EnemyAnimationSpec.once("Death.png", 0, 0, 0.18f))
                .build());
    }
}


