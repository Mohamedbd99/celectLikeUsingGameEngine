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
                .stats(new EnemyStats(150, 60f, 40f, 20, 60f, 1.2f)) // health, contactRadius, moveSpeed, attackDamage, attackRange, attackCooldown
                .animation(EnemyAnimationKey.IDLE,
                        EnemyAnimationSpec.looping("IDLE.png", redDeonWidth, redDeonHeight, 0.1f))
                .animation(EnemyAnimationKey.HURT,
                        EnemyAnimationSpec.once("HURT.png", redDeonWidth, redDeonHeight, 0.08f))
                .animation(EnemyAnimationKey.ATTACK,
                        EnemyAnimationSpec.once("ATTACK.png", redDeonWidth, redDeonHeight, 0.08f))
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
        float idleDuration = 1f / 6f; // 6 fps
        float walkDuration = 1f / 10f; // 10 fps        
        float shieldDuration = 1f / 8f; // 8 fps
        float hitDuration = 1f / 12f; // 12 fps
        float deathDuration = 1f / 8f; // 8 fps
        float attackDuration = 1f / 12f; // 12 fps
        register(EnemyDefinition.builder("skeletonEnemie", skeletonBase)
        .stats(new EnemyStats(220, 13f, 35f, 18, 55f, 1.5f)) // health, contactRadius, moveSpeed, attackDamage, attackRange, attackCooldown
        .animation(EnemyAnimationKey.IDLE,
            EnemyAnimationSpec.looping("Idle.png", 150, 150, idleDuration,0f,-45f))
        // ⚠️ ATTACK -> once (sinon attaque en boucle)
        .animation(EnemyAnimationKey.ATTACK,
            EnemyAnimationSpec.once("Attack.png", 150, 150, attackDuration,0f,-45f))
        .animation(EnemyAnimationKey.HURT,
            EnemyAnimationSpec.once("Take Hit.png", 150, 150, hitDuration,0f,-45f))
        .animation(EnemyAnimationKey.DEATH,
            EnemyAnimationSpec.once("Death.png", 150, 150, deathDuration,0f,-45f))
        .animation(EnemyAnimationKey.FLY,
            EnemyAnimationSpec.looping("Walk.png", 150, 150, walkDuration,0f,-45f))
        .build());
        }
}


  
