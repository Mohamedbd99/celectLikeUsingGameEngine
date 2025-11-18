package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.LevelData.TileBlueprint;
import org.example.samurai.SamuraiCharacter;

/**
 * Minimal runtime that renders the currently authored level.
 * The interactive editor logic now lives in org.example.editor.ViewEditor.
 */
public class CelesteGame extends ApplicationAdapter {

    private static final String KENNEY_BASE = "assets/kenney_pico-8-platformer/";
    private static final float TILE_SCALE = 4f; // 8px -> 32 px world units

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private final List<Texture> paletteTextures = new ArrayList<>();
    private final List<TextureRegion> paletteRegions = new ArrayList<>();
    private TileCell[][] cells;
    private float tileWorldSize;
    private float worldWidth;
    private float worldHeight;
    private float elapsed;
    private float viewWidth;
    private float viewHeight;
    private SamuraiCharacter samurai;

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

        worldWidth = cols * tileWorldSize;
        worldHeight = rows * tileWorldSize;
        viewWidth = worldWidth;
        viewHeight = worldHeight;

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
        samurai.ensureIdleState();
        Gdx.app.log("CelesteGame", "Samurai initialized at (" + spawnX + ", " + spawnY + ")");
    }

    private void updateCamera() {
        if (camera == null) {
            return;
        }
        float centerX = worldWidth * 0.5f;
        float centerY = worldHeight * 0.5f;
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
