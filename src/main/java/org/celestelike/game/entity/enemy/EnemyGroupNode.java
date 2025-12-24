package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class EnemyGroupNode implements EnemyComponent {

    private final String name;
    private final List<EnemyComponent> children = new ArrayList<>();

    EnemyGroupNode(String name) {
        this.name = name;
    }

    void add(EnemyComponent component) {
        children.add(component);
    }

    void clear() {
        for (EnemyComponent child : children) {
            child.dispose();
        }
        children.clear();
    }

    @Override
    public void update(float delta) {
        for (EnemyComponent child : children) {
            child.update(delta);
        }
        purgeEmpty();
    }
    
    public void updateWithPlayer(float delta, Vector2 playerPosition) {
        for (EnemyComponent child : children) {
            if (child instanceof EnemyLeafNode) {
                ((EnemyLeafNode) child).update(delta, playerPosition);
            } else if (child instanceof EnemyGroupNode) {
                ((EnemyGroupNode) child).updateWithPlayer(delta, playerPosition);
            } else {
                child.update(delta);
            }
        }
        purgeEmpty();
    }
    
    public void checkEnemyAttacks(Vector2 playerPosition, Object player) {
        for (EnemyComponent child : children) {
            if (child instanceof EnemyLeafNode) {
                ((EnemyLeafNode) child).checkAttack(playerPosition, player);
            } else if (child instanceof EnemyGroupNode) {
                ((EnemyGroupNode) child).checkEnemyAttacks(playerPosition, player);
            }
        }
    }

    private void purgeEmpty() {
        for (Iterator<EnemyComponent> iterator = children.iterator(); iterator.hasNext(); ) {
            EnemyComponent component = iterator.next();
            if (component.isEmpty()) {
                component.dispose();
                iterator.remove();
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        for (EnemyComponent child : children) {
            child.draw(batch);
        }
    }

    @Override
    public void drawHealthFill(ShapeRenderer renderer) {
        for (EnemyComponent child : children) {
            child.drawHealthFill(renderer);
        }
    }

    @Override
    public void drawHealthOutline(ShapeRenderer renderer) {
        for (EnemyComponent child : children) {
            child.drawHealthOutline(renderer);
        }
    }

    @Override
    public boolean applyMeleeDamage(
            Vector2 attackPoint,
            float attackRadius,
            int damage,
            EnemyManager.EnemyEventListener listener) {
        for (EnemyComponent child : children) {
            if (child.applyMeleeDamage(attackPoint, attackRadius, damage, listener)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        for (EnemyComponent child : children) {
            child.dispose();
        }
        children.clear();
    }

    @Override
    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public String toString() {
        return "EnemyGroupNode{" + name + ", children=" + children.size() + '}';
    }
}

