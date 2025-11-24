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
        // redDeon definition
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
                        EnemyAnimationSpec.once("DEATH.png", redDeonWidth, redDeonHeight, 0.2f))
                .animation(EnemyAnimationKey.FLY,
                        EnemyAnimationSpec.looping("FLYING.png", redDeonWidth, redDeonHeight, 0.08f))
                .build());

        // skeletonEnemie definition. Most sheets are 150px tall (Walk / Attack / Death / Hit),
        // but the Idle sheet currently has a smaller height, so we use a tighter frame height
        // there to match the actual texture and avoid producing zero frames.
        String skeletonBase = "assets/emenies/skeletonEnemie/";
        int skeletonFrame = 150;

        register(EnemyDefinition.builder("skeletonEnemie", skeletonBase)
                .stats(new EnemyStats(220, 50f))
                // Idle.png: use a shorter frame height to match the actual spritesheet
                // (otherwise TextureRegion.split would see 0 rows and produce no frames).
                .animation(EnemyAnimationKey.IDLE,
                        EnemyAnimationSpec.looping("Idle.png", skeletonFrame, 50, 0.1f,
                                30f, 0f))      // shift slightly left
                .animation(EnemyAnimationKey.ATTACK,
                        EnemyAnimationSpec.looping("Attack.png", skeletonFrame, skeletonFrame, 0.12f,
                                0, 0))
                .animation(EnemyAnimationKey.HURT,
                        EnemyAnimationSpec.once("Take Hit.png", skeletonFrame, 50, 0.08f,
                                -35f, 0))
                .animation(EnemyAnimationKey.DEATH,
                        EnemyAnimationSpec.once("Death.png", skeletonFrame, skeletonFrame, 0.18f,
                                0, 0))
                .animation(EnemyAnimationKey.FLY,
                        EnemyAnimationSpec.looping("Walk.png", skeletonFrame, skeletonFrame, 0.14f,
                                0, 0))
                .build());                
    }
}


