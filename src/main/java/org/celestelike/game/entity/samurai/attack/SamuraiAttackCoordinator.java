package org.celestelike.game.entity.samurai.attack;

import org.celestelike.game.entity.samurai.SamuraiCharacter;

/**
 * Manages combo progression and chooses the appropriate attack strategy
 * based on current context.
 */
public final class SamuraiAttackCoordinator {

    private static final float COMBO_RESET_TIME = 0.6f;

    private final GroundAttackOneStrategy attackOne = new GroundAttackOneStrategy();
    private final GroundAttackTwoStrategy attackTwo = new GroundAttackTwoStrategy();
    private final GroundAttackThreeStrategy attackThree = new GroundAttackThreeStrategy();
    private final AirAttackStrategy airAttack = new AirAttackStrategy();

    private float comboTimer;
    private int lastGroundAttackIndex; // 0 none, 1..3

    public SamuraiAttackStrategy requestStrategy(SamuraiCharacter samurai) {
        if (!samurai.isGrounded()) {
            resetCombo();
            return airAttack.canExecute(samurai) ? airAttack : null;
        }
        int desiredIndex;
        if (comboTimer <= 0f || lastGroundAttackIndex == 0) {
            desiredIndex = 1;
        } else if (lastGroundAttackIndex >= 3) {
            desiredIndex = 1;
        } else {
            desiredIndex = lastGroundAttackIndex + 1;
        }
        return switch (desiredIndex) {
            case 1 -> attackOne;
            case 2 -> attackTwo;
            default -> attackThree;
        };
    }

    public void onAttackStarted(SamuraiAttackStrategy strategy, boolean grounded) {
        if (!grounded || strategy == airAttack) {
            resetCombo();
            return;
        }
        if (strategy == attackOne) {
            lastGroundAttackIndex = 1;
        } else if (strategy == attackTwo) {
            lastGroundAttackIndex = 2;
        } else if (strategy == attackThree) {
            lastGroundAttackIndex = 3;
        } else {
            lastGroundAttackIndex = 0;
        }
        comboTimer = COMBO_RESET_TIME;
    }

    public void onAttackComplete(boolean grounded) {
        if (!grounded) {
            resetCombo();
        }
    }

    public void tick(float delta, boolean freezeTimer) {
        if (freezeTimer) {
            return;
        }
        if (comboTimer > 0f) {
            comboTimer = Math.max(0f, comboTimer - delta);
            if (comboTimer == 0f) {
                lastGroundAttackIndex = 0;
            }
        }
    }

    public void resetCombo() {
        comboTimer = 0f;
        lastGroundAttackIndex = 0;
    }
}


