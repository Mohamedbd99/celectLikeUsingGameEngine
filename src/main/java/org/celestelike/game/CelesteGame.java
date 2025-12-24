package org.celestelike.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.celestelike.game.config.GameConfig;
import org.celestelike.game.entity.samurai.SamuraiCharacter;
import org.celestelike.game.entity.samurai.input.AttackCommand;
import org.celestelike.game.entity.samurai.input.DashCommand;
import org.celestelike.game.entity.samurai.input.DefendCommand;
import org.celestelike.game.entity.samurai.input.SpecialAttackCommand;
import org.celestelike.game.entity.samurai.input.JumpCommand;
import org.celestelike.game.entity.samurai.input.MoveDownCommand;
import org.celestelike.game.entity.samurai.input.MoveLeftCommand;
import org.celestelike.game.entity.samurai.input.MoveRightCommand;
import org.celestelike.game.entity.samurai.input.MoveUpCommand;
import org.celestelike.game.entity.samurai.input.SamuraiCommand;
import org.celestelike.game.entity.enemy.EnemyDefinition;
import org.celestelike.game.entity.enemy.EnemyManager;
import org.celestelike.game.entity.enemy.EnemySpawn;
import org.celestelike.game.entity.enemy.EnemySpawnLoader;
import org.celestelike.game.logging.GameLogger;
import org.celestelike.game.state.GameState;
import org.celestelike.game.entity.samurai.powerup.SamuraiPowerUpSnapshot;
import org.celestelike.game.entity.samurai.powerup.SamuraiPowerUpType;
import org.celestelike.game.world.LevelCollisionMap;
import org.celestelike.game.world.LevelData;
import org.celestelike.game.world.LevelData.TileBlueprint;
import org.celestelike.game.world.TilesetIO;
import org.celestelike.game.world.TilesetIO.TilesetData;

/**
 * Minimal runtime that renders the currently authored level.
 * External tools are responsible for authoring the blueprint and collision data.
 */
public class CelesteGame extends ApplicationAdapter {

    private static final String TILESET_BASE = "assets/newTileSetManara/";
    private static final String TILESET_TSX = "assets/b.tsx";
    private static final float TILE_SCALE = 4f; // 8px -> 32 px world units
    private static final float VIEW_TILES_W = 28f;
    private static final float VIEW_TILES_H = 16f;
    private static final int SCORE_PER_ENEMY = 100;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private final List<Texture> paletteTextures = new ArrayList<>();
    private final List<TextureRegion> paletteRegions = new ArrayList<>();
    private TileCell[][] cells;
    private LevelCollisionMap collisionMap;
    private float tileWorldSize;
    private float worldWidth;
    private float worldHeight;
    private float elapsed;
    private float viewTilesWide = VIEW_TILES_W;
    private float viewTilesTall = VIEW_TILES_H;
    private float cameraZoom = 1f;
    private float viewWidth;
    private float viewHeight;
    private GameConfig gameConfig;
    private SamuraiCharacter samurai;
    private ShapeRenderer uiShape;
    private final Matrix4 uiMatrix = new Matrix4();
    private SamuraiCommand moveRightCommand;
    private SamuraiCommand moveLeftCommand;
    private SamuraiCommand moveUpCommand;
    private SamuraiCommand moveDownCommand;
    private SamuraiCommand jumpCommand;
    private DashCommand dashCommand;
    private SamuraiCommand attackCommand;
    private SamuraiCommand defendCommand;
    private SamuraiCommand specialAttackCommand;
    private float spawnX;
    private float spawnY;
    private EnemyManager enemyManager;
    private List<EnemySpawn> enemySpawns = new ArrayList<>();
    private int blueprintRows;
    private GameState currentGameState = GameState.MENU;
    private BitmapFont hudFont;
    private float playTimer;
    private int score;
    private final GlyphLayout hudLayout = new GlyphLayout();
    private boolean enemiesWereSpawned = false; // Track if enemies were spawned in current run

    @Override
    public void create() {
        gameConfig = GameConfig.load();
        tileWorldSize = LevelData.TILE_SIZE * TILE_SCALE;
        GameLogger.info("Game started");

        // Use a fixed, designer-controlled framing; no dynamic zoom changes at runtime.
        viewTilesWide = gameConfig.cameraTilesWide();
        viewTilesTall = gameConfig.cameraTilesTall();
        cameraZoom = 1f; // ignore config zoom so camera size stays stable

        TileBlueprint[][] blueprint = LevelData.copyBlueprint();
        blueprintRows = blueprint.length;
        int cols = blueprint[0].length;
        cells = new TileCell[blueprintRows][cols];
        for (int row = 0; row < blueprintRows; row++) {
            for (int col = 0; col < cols; col++) {
                cells[row][col] = TileCell.fromBlueprint(blueprint[row][col]);
            }
        }
        collisionMap = new LevelCollisionMap(blueprint, tileWorldSize);

        worldWidth = cols * tileWorldSize;
        worldHeight = blueprintRows * tileWorldSize;

        // Show the whole authored level: viewport matches world size.
        viewTilesWide = cols;
        viewTilesTall = blueprintRows;
        viewWidth = worldWidth;
        viewHeight = worldHeight;

        camera = new OrthographicCamera();
        viewport = new FitViewport(viewWidth, viewHeight, camera);
        viewport.apply(true);
        camera.zoom = cameraZoom;

        batch = new SpriteBatch();
        uiShape = new ShapeRenderer();
        hudFont = new BitmapFont();
        hudFont.getData().setScale(1.1f);
        loadTileset();
        initSamurai();
        enemyManager = new EnemyManager();
        enemyManager.setEventListener(this::handleEnemyDefeated);
        enemySpawns = EnemySpawnLoader.load();
        if (!enemySpawns.isEmpty()) {
            enemyManager.spawnAll(enemySpawns, blueprintRows, tileWorldSize);
        }
        samurai.setAttackImpactListener(damage -> {
            if (enemyManager != null) {
                enemyManager.applyMeleeDamage(samurai, damage);
            }
        });
        Gdx.graphics.setVSync(true);
        updateCamera();
        transitionGameState(GameState.MENU);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        elapsed += delta;
        ScreenUtils.clear(0.08f, 0.08f, 0.12f, 1f);

        handleInput(delta);

        float simulationDelta = currentGameState == GameState.PLAYING ? delta : 0f;
        updateWorld(simulationDelta);
        if (currentGameState == GameState.PLAYING) {
            playTimer += delta;
        }

        updateCamera();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawTiles();
        if (enemyManager != null) {
            enemyManager.draw(batch);
        }
        drawSamurai();
        batch.end();
        if (enemyManager != null && uiShape != null) {
            uiShape.setProjectionMatrix(camera.combined);
            enemyManager.drawHealthBars(uiShape);
        }
        drawHudOverlay();
        drawStateOverlay();
    }

    private void updateWorld(float delta) {
        if (samurai != null) {
            samurai.update(delta);
        }
        if (enemyManager != null) {
            if (samurai != null && !samurai.isDead()) {
                enemyManager.update(delta, samurai.getPosition());
                enemyManager.checkEnemyAttacks(samurai);
            } else {
                enemyManager.update(delta);
            }
            // Only check for victory if enemies were actually spawned in this run
            if (delta > 0f && currentGameState == GameState.PLAYING && enemiesWereSpawned && enemyManager.isEmpty()) {
                transitionGameState(GameState.VICTORY);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (currentGameState != GameState.GAME_OVER) {
            transitionGameState(GameState.GAME_OVER);
        }
        GameLogger.info("Game terminated");
        batch.dispose();
        if (uiShape != null) {
            uiShape.dispose();
        }
        if (hudFont != null) {
            hudFont.dispose();
        }
        for (Texture texture : paletteTextures) {
            texture.dispose();
        }
        if (samurai != null) {
            samurai.dispose();
        }
        if (enemyManager != null) {
            enemyManager.dispose();
        }
    }

    private void drawTiles() {
        int rows = cells.length;
        int cols = cells[0].length;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileCell cell = cells[row][col];
                TextureRegion frame = cell.getFrame(paletteRegions, elapsed);
                if (frame == null) {
                    continue;
                }
                float x = col * tileWorldSize;
                float y = row * tileWorldSize;
                batch.draw(frame, x, y, tileWorldSize, tileWorldSize);
            }
        }
    }

    private void drawSamurai() {
        if (samurai == null) {
            return;
        }
        samurai.draw(batch);
    }

    private void loadTileset() {
        paletteTextures.clear();
        paletteRegions.clear();
        TilesetData data = TilesetIO.loadFromTsx(TILESET_TSX);
        if (!data.isEmpty()) {
            paletteTextures.addAll(data.textures());
            paletteRegions.addAll(data.regions());
        } else {
            loadPaletteFromTilesDirectory();
            if (paletteRegions.isEmpty()) {
                loadPaletteFromAtlas();
            }
        }
        if (paletteRegions.isEmpty()) {
            throw new IllegalStateException("Unable to load tile palette.");
        }
    }

    private void loadPaletteFromTilesDirectory() {
        FileHandle directory = Gdx.files.internal(TILESET_BASE);
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Gdx.app.log("CelesteGame", "Tileset folder " + TILESET_BASE + " missing.");
            return;
        }
        FileHandle[] contents = directory.list();
        if (contents == null || contents.length == 0) {
            return;
        }
        Arrays.sort(contents, (a, b) -> a.name().compareToIgnoreCase(b.name()));
        int loaded = 0;
        for (FileHandle file : contents) {
            String extension = file.extension().toLowerCase(Locale.ROOT);
            if (!"png".equals(extension)) {
                continue;
            }
            Texture texture = new Texture(file);
            paletteTextures.add(texture);
            paletteRegions.add(new TextureRegion(texture));
            loaded++;
        }
        if (loaded > 0) {
            Gdx.app.log("CelesteGame", "Loaded " + paletteRegions.size() + " textures from " + TILESET_BASE);
        }
    }

    private void loadPaletteFromAtlas() {
        FileHandle file = Gdx.files.internal(TILESET_BASE + "tilemap.png");
        if (!file.exists()) {
            return;
        }
        Texture atlas = new Texture(file);
        paletteTextures.add(atlas);
        int tileSize = LevelData.TILE_SIZE;
        int rows = atlas.getHeight() / tileSize;
        int cols = atlas.getWidth() / tileSize;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TextureRegion region = new TextureRegion(atlas,
                        col * tileSize,
                        row * tileSize,
                        tileSize,
                        tileSize);
                paletteRegions.add(region);
            }
        }
        Gdx.app.log("CelesteGame", "Loaded " + paletteRegions.size() + " sprites from atlas fallback");
    }

    private void initSamurai() {
        samurai = new SamuraiCharacter(gameConfig.player());
        samurai.loadAssets();
        spawnX = 1f * tileWorldSize ;
        spawnY = 2f * tileWorldSize;
        samurai.placeAt(spawnX, spawnY);
        samurai.configurePhysics(-1800f, 0f);
        samurai.attachCollisionMap(collisionMap);
        samurai.ensureIdleState();
        samurai.setDeathListener(this::handleSamuraiDeath);
        moveRightCommand = new MoveRightCommand();
        moveLeftCommand = new MoveLeftCommand();
        moveUpCommand = new MoveUpCommand();
        moveDownCommand = new MoveDownCommand();
        jumpCommand = new JumpCommand();
        dashCommand = new DashCommand();
        attackCommand = new AttackCommand();
        defendCommand = new DefendCommand();
        specialAttackCommand = new SpecialAttackCommand();
        Gdx.app.log("CelesteGame", "Samurai initialized at (" + spawnX + ", " + spawnY + ")");
        GameLogger.entityCreated("Samurai", "player");
    }

    private void handleInput(float delta) {
        switch (currentGameState) {
            case MENU -> handleMenuInput();
            case PLAYING -> handlePlayingInput(delta);
            case PAUSED -> handlePauseInput();
            case GAME_OVER -> handleGameOverInput();
            case VICTORY -> handleVictoryInput();
        }
    }

    private void handlePlayingInput(float delta) {
        if (samurai == null
                || moveRightCommand == null
                || moveLeftCommand == null
                || moveUpCommand == null
                || moveDownCommand == null
                || jumpCommand == null
                || dashCommand == null
                || attackCommand == null
                || defendCommand == null
                || specialAttackCommand == null) {
            return;
        }
        boolean leftHeld = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean rightHeld = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
        boolean upHeld = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        boolean downHeld = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);

        if (leftHeld && !rightHeld) {
            moveLeftCommand.execute(samurai, delta);
            moveRightCommand.release(samurai);
        } else if (rightHeld && !leftHeld) {
            moveRightCommand.execute(samurai, delta);
            moveLeftCommand.release(samurai);
        } else {
            moveLeftCommand.release(samurai);
            moveRightCommand.release(samurai);
        }

        if (upHeld && !downHeld) {
            moveUpCommand.execute(samurai, delta);
            moveDownCommand.release(samurai);
        } else if (downHeld && !upHeld) {
            moveDownCommand.execute(samurai, delta);
            moveUpCommand.release(samurai);
        } else {
            moveUpCommand.release(samurai);
            moveDownCommand.release(samurai);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            jumpCommand.execute(samurai, delta);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.J)
                || Gdx.input.isKeyJustPressed(Input.Keys.Z)
                || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            attackCommand.execute(samurai, delta);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isButtonJustPressed(Input.Buttons.MIDDLE)) {
            specialAttackCommand.execute(samurai, delta);
        }

        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
            defendCommand.execute(samurai, delta);
        } else {
            defendCommand.release(samurai);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
            float dashX = 0f;
            if (rightHeld) {
                dashX += 1f;
            }
            if (leftHeld) {
                dashX -= 1f;
            }
            float dashY = 0f;
            if (upHeld) {
                dashY += 1f;
            }
            if (downHeld) {
                dashY -= 1f;
            }
            dashCommand.setDirection(dashX, dashY);
            dashCommand.execute(samurai, delta);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            transitionGameState(GameState.PAUSED);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            samurai.grantPowerUp(SamuraiPowerUpType.SHIELD);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            samurai.grantPowerUp(SamuraiPowerUpType.SPEED);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            samurai.grantPowerUp(SamuraiPowerUpType.WEAPON);
        }
    }

    private void handleMenuInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startNewRun();
        }
    }

    private void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            transitionGameState(GameState.PLAYING);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            transitionGameState(GameState.MENU);
        }
    }

    private void handleGameOverInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            startNewRun();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            transitionGameState(GameState.MENU);
        }
    }

    private void handleVictoryInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            startNewRun();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            transitionGameState(GameState.MENU);
        }
    }

    private void updateCamera() {
        if (camera == null) {
            return;
        }
        // Fixed camera: keep view centered on the level, no player-follow POV.
        float centerX = worldWidth * 0.5f;
        float centerY = worldHeight * 0.5f;
        camera.position.set(centerX, centerY, 0f);
    }

    private void handleSamuraiDeath() {
        GameLogger.entityDestroyed("Samurai", "player");
        transitionGameState(GameState.GAME_OVER);
    }

    private void handleEnemyDefeated(EnemyDefinition definition) {
        score += SCORE_PER_ENEMY;
        GameLogger.info("Enemy defeated: " + definition.id() + " (score=" + score + ")");
    }

    private void startNewRun() {
        score = 0;
        playTimer = 0f;
        enemiesWereSpawned = false; // Reset flag
        if (samurai != null) {
            samurai.reviveAt(spawnX, spawnY);
            GameLogger.entityCreated("Samurai", "player");
        }
        if (enemyManager != null) {
            enemyManager.respawn(enemySpawns, blueprintRows, tileWorldSize);
            // Check if enemies were actually spawned (not empty after respawn)
            enemiesWereSpawned = !enemyManager.isEmpty();
        }
        transitionGameState(GameState.PLAYING);
    }

    private void drawHudOverlay() {
        if (uiShape == null || samurai == null || hudFont == null) {
            return;
        }
        int max = samurai.getMaxHealth();
        if (max <= 0) {
            return;
        }
        int current = Math.max(0, samurai.getCurrentHealth());
        float ratio = MathUtils.clamp(current / (float) max, 0f, 1f);
        float margin = 20f;
        float barWidth = 260f;
        float barHeight = 18f;
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        uiShape.setProjectionMatrix(uiMatrix.setToOrtho2D(0f, 0f, width, height));
        uiShape.begin(ShapeRenderer.ShapeType.Filled);
        float x = margin;
        float y = height - margin - barHeight;
        uiShape.setColor(0f, 0f, 0f, 0.6f);
        uiShape.rect(x - 2f, y - 2f, barWidth + 4f, barHeight + 4f);
        uiShape.setColor(0.18f, 0.18f, 0.18f, 0.85f);
        uiShape.rect(x, y, barWidth, barHeight);
        uiShape.setColor(0.85f, 0.1f, 0.2f, 0.95f);
        uiShape.rect(x, y, barWidth * ratio, barHeight);
        uiShape.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        float textY = height - margin - barHeight - 10f;
        hudFont.draw(batch, "Score: " + score, margin, textY);
        hudFont.draw(batch, "Time : " + formatClock(playTimer), margin, textY - 20f);
        List<SamuraiPowerUpSnapshot> buffs = samurai.getActivePowerUps();
        float powerY = textY - 50f;
        for (SamuraiPowerUpSnapshot buff : buffs) {
            String label = buff.type().displayName() + " (" + String.format(Locale.US, "%.1fs", Math.max(0f, buff.remainingSeconds())) + ")";
            hudFont.draw(batch, label, margin, powerY);
            powerY -= 18f;
        }
        batch.end();
    }

    private void drawStateOverlay() {
        if (hudFont == null || currentGameState == GameState.PLAYING) {
            return;
        }
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        uiShape.setProjectionMatrix(uiMatrix.setToOrtho2D(0f, 0f, width, height));
        uiShape.begin(ShapeRenderer.ShapeType.Filled);
        uiShape.setColor(0f, 0f, 0f, 0.55f);
        uiShape.rect(0f, 0f, width, height);
        uiShape.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        String title;
        String subtitle;
        switch (currentGameState) {
            case MENU -> {
                title = "Press ENTER to begin";
                subtitle = "Controls: WASD move, SPACE jump, J attack, E special, RMB defend";
            }
            case PAUSED -> {
                title = "Paused";
                subtitle = "Press ESC to resume or M for menu";
            }
            case GAME_OVER -> {
                title = "Game Over";
                subtitle = "Press ENTER to retry or M for menu";
            }
            case VICTORY -> {
                title = "Victory!";
                subtitle = "Press ENTER for another run";
            }
            default -> {
                title = "";
                subtitle = "";
            }
        }
        hudLayout.setText(hudFont, title);
        hudFont.draw(batch, hudLayout, (width - hudLayout.width) * 0.5f, height * 0.6f);
        hudLayout.setText(hudFont, subtitle);
        hudFont.draw(batch, hudLayout, (width - hudLayout.width) * 0.5f, height * 0.6f - 30f);
        batch.end();
    }

    private String formatClock(float seconds) {
        int total = Math.max(0, (int) seconds);
        int minutes = total / 60;
        int secs = total % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, secs);
    }

    private void transitionGameState(GameState next) {
        if (next == null || next == currentGameState) {
            return;
        }
        String prev = currentGameState == null ? "NONE" : currentGameState.name();
        GameLogger.stateTransition("Game", prev, next.name());
        currentGameState = next;
    }

    private static class TileCell {
        static final int MAX_FRAMES = 3;

        final int[] frameIndices = new int[MAX_FRAMES];
        int frameCount;
        float frameDuration = 0.15f;

        static TileCell fromBlueprint(TileBlueprint blueprint) {
            TileCell cell = new TileCell();
            cell.setFrames(blueprint.frames(), blueprint.frameDuration());
            return cell;
        }

        void setFrames(int[] frames, float duration) {
            frameCount = Math.min(frames.length, MAX_FRAMES);
            for (int i = 0; i < frameCount; i++) {
                frameIndices[i] = frames[i];
            }
            frameDuration = duration;
        }

        TextureRegion getFrame(List<TextureRegion> palette, float time) {
            if (frameCount <= 0) {
                return null;
            }
            float duration = Math.max(frameDuration, 0.01f);
            int frame = (int) Math.floor((time / duration) % frameCount);
            int index = frameIndices[frame];
            if (index < 0 || index >= palette.size()) {
                return null;
            }
            return palette.get(index);
        }
    }
}
