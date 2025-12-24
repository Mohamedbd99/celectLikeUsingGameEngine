package org.celestelike.game.entity.enemy;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

interface EnemyComponent {

    void update(float delta);

    void draw(SpriteBatch batch);

    void drawHealthFill(ShapeRenderer renderer);

    void drawHealthOutline(ShapeRenderer renderer);

    /**
     * @return true if damage connected with any enemy.
     */
    boolean applyMeleeDamage(Vector2 attackPoint, float attackRadius, int damage, EnemyManager.EnemyEventListener listener);

    void dispose();

    boolean isEmpty();
}

