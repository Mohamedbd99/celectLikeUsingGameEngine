package org.celestelike.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.celestelike.game.entity.samurai.SamuraiCharacter;
import org.celestelike.game.entity.samurai.input.AttackCommand;
import org.celestelike.game.entity.samurai.input.DashCommand;
import org.celestelike.game.entity.samurai.input.DefendCommand;
import org.celestelike.game.entity.samurai.input.JumpCommand;
import org.celestelike.game.entity.samurai.input.MoveDownCommand;
import org.celestelike.game.entity.samurai.input.MoveLeftCommand;
import org.celestelike.game.entity.samurai.input.MoveRightCommand;
import org.celestelike.game.entity.samurai.input.MoveUpCommand;
import org.celestelike.game.entity.samurai.input.SamuraiCommand;
import org.celestelike.game.world.LevelCollisionMap;
import org.celestelike.game.world.LevelData;
import org.celestelike.game.world.LevelData.TileBlueprint;

/**
 * Minimal runtime that renders the currently authored level.
 * The interactive editor logic now lives in org.celestelike.tools.editor.ViewEditor.
 */
public class CelesteGame extends ApplicationAdapter {

    private static final String KENNEY_BASE = "assets/kenney_pico-8-platformer/";
    private static final float TILE_SCALE = 4f; // 8px -> 32 px world units
    private static final float VIEW_TILES_W = 28f;
    private static final float VIEW_TILES_H = 16f;

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
    private float viewWidth;
    private float viewHeight;
    private SamuraiCharacter samurai;
    private SamuraiCommand moveRightCommand;
    private SamuraiCommand moveLeftCommand;
    private SamuraiCommand moveUpCommand;
    private SamuraiCommand moveDownCommand;
    private SamuraiCommand jumpCommand;
    private DashCommand dashCommand;
    private SamuraiCommand attackCommand;
    private SamuraiCommand defendCommand;

    @Override
    public void create() {
        tileWorldSize = LevelData.TILE_SIZE * TILE_SCALE;

        TileBlueprint[][] blueprint = LevelData.copyBlueprint();
        int rows = blueprint.length;
        int cols = blueprint[0].length;
        cells = new TileCell[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                cells[row][col] = TileCell.fromBlueprint(blueprint[row][col]);
            }
        }
        collisionMap = new LevelCollisionMap(blueprint, tileWorldSize);

        worldWidth = cols * tileWorldSize;
        worldHeight = rows * tileWorldSize;
        viewWidth = VIEW_TILES_W * tileWorldSize;
        viewHeight = VIEW_TILES_H * tileWorldSize;

        camera = new OrthographicCamera();
        viewport = new FitViewport(viewWidth, viewHeight, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        loadTileset();
        initSamurai();
        Gdx.graphics.setVSync(true);
        updateCamera();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        elapsed += delta;
        ScreenUtils.clear(0.08f, 0.08f, 0.12f, 1f);

        handleInput(delta);

        if (samurai != null) {
            samurai.update(delta);
        }

        updateCamera();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawTiles();
        drawSamurai();
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        for (Texture texture : paletteTextures) {
            texture.dispose();
        }
        if (samurai != null) {
            samurai.dispose();
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
                float y = (rows - row - 1) * tileWorldSize;
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
        loadPaletteFromTilesDirectory();
        if (paletteRegions.isEmpty()) {
            loadPaletteFromAtlas();
        }
        if (paletteRegions.isEmpty()) {
            throw new IllegalStateException("Unable to load tile palette.");
        }
    }

    private void loadPaletteFromTilesDirectory() {
        String basePath = KENNEY_BASE + "Transparent/Tiles/";
        boolean any = false;
        int consecutiveMisses = 0;
        for (int index = 0; index < 512 && consecutiveMisses < 50; index++) {
            String name = String.format(Locale.US, "tile_%04d.png", index);
            FileHandle file = Gdx.files.internal(basePath + name);
            if (!file.exists()) {
                consecutiveMisses++;
                continue;
            }
            Texture texture = new Texture(file);
            paletteTextures.add(texture);
            paletteRegions.add(new TextureRegion(texture));
            consecutiveMisses = 0;
            any = true;
        }
        if (any) {
            Gdx.app.log("CelesteGame", "Loaded " + paletteRegions.size() + " sprites from Transparent/Tiles");
        }
    }

    private void loadPaletteFromAtlas() {
        FileHandle file = Gdx.files.internal(KENNEY_BASE + "Transparent/Tilemap/tilemap.png");
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
        samurai = new SamuraiCharacter();
        samurai.loadAssets();
        float spawnX = 1f * tileWorldSize - 3f;
        float spawnY = 2f * tileWorldSize;
        samurai.placeAt(spawnX, spawnY);
        samurai.configurePhysics(-1800f, 0f);
        samurai.attachCollisionMap(collisionMap);
        samurai.ensureIdleState();
        moveRightCommand = new MoveRightCommand();
        moveLeftCommand = new MoveLeftCommand();
        moveUpCommand = new MoveUpCommand();
        moveDownCommand = new MoveDownCommand();
        jumpCommand = new JumpCommand();
        dashCommand = new DashCommand();
        attackCommand = new AttackCommand();
        defendCommand = new DefendCommand();
        Gdx.app.log("CelesteGame", "Samurai initialized at (" + spawnX + ", " + spawnY + ")");
    }

    private void handleInput(float delta) {
        if (samurai == null
                || moveRightCommand == null
                || moveLeftCommand == null
                || moveUpCommand == null
                || moveDownCommand == null
                || jumpCommand == null
                || dashCommand == null
                || attackCommand == null
                || defendCommand == null) {
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
    }

    private void updateCamera() {
        if (camera == null) {
            return;
        }
        float centerX = worldWidth * 0.5f;
        float centerY = worldHeight * 0.5f;
        if (samurai != null) {
            var samuraiPos = samurai.getPosition();
            centerX = samuraiPos.x + tileWorldSize * 0.5f;
            centerY = samuraiPos.y + tileWorldSize * 0.5f;
        }
        float halfWidth = camera.viewportWidth * 0.5f;
        float halfHeight = camera.viewportHeight * 0.5f;
        if (worldWidth > camera.viewportWidth) {
            centerX = MathUtils.clamp(centerX, halfWidth, worldWidth - halfWidth);
        } else {
            centerX = worldWidth * 0.5f;
        }
        if (worldHeight > camera.viewportHeight) {
            centerY = MathUtils.clamp(centerY, halfHeight, worldHeight - halfHeight);
        } else {
            centerY = worldHeight * 0.5f;
        }
        camera.position.set(centerX, centerY, 0f);
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
