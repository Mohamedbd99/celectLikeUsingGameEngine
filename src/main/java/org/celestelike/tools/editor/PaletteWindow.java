package org.celestelike.tools.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.celestelike.game.world.LevelData;
import org.celestelike.game.world.TilesetIO;
import org.celestelike.game.world.TilesetIO.TilesetData;

/**
 * Dedicated window that renders the tileset palette so the map view is never obscured.
 */
final class PaletteWindow extends ApplicationAdapter {

    private static final float HEADER_HEIGHT = 64f;
    private static final float PADDING = 18f;

    private final String tsxPath;
    private final String fallbackDirectory;
    private final float paletteScale;
    private final PaletteSharedState sharedState;

    private final List<Texture> paletteTextures = new ArrayList<>();
    private final List<TextureRegion> paletteRegions = new ArrayList<>();

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private final Matrix4 uiMatrix = new Matrix4();

    private float tileSize;
    private int paletteCols = 1;
    private int paletteRows = 1;
    private float scrollOffset;
    private float pendingScroll;
    private int hoverIndex = -1;
    private boolean inputProcessorAttached;
    private final InputAdapter scrollAdapter = new InputAdapter() {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            pendingScroll += amountY;
            return true;
        }
    };

    PaletteWindow(String tsxPath, String fallbackDirectory, float paletteScale, PaletteSharedState sharedState) {
        this.tsxPath = tsxPath;
        this.fallbackDirectory = fallbackDirectory;
        this.paletteScale = paletteScale;
        this.sharedState = sharedState;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.05f);
        tileSize = LevelData.TILE_SIZE * paletteScale;
        loadTileset();
        Gdx.input.setInputProcessor(scrollAdapter);
        inputProcessorAttached = true;
    }

    @Override
    public void render() {
        if (paletteRegions.isEmpty()) {
            ScreenUtils.clear(1f, 1f, 1f, 1f);
            return;
        }
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        uiMatrix.setToOrtho2D(0f, 0f, width, height);
        batch.setProjectionMatrix(uiMatrix);
        shapes.setProjectionMatrix(uiMatrix);

        updateGridLayout(width);
        handleScrollInput(height);
        clampScroll(height);
        updateHover(width, height);
        handleClickInput();

        ScreenUtils.clear(1f, 1f, 1f, 1f);
        drawHeader(width, height);
        drawTiles(width, height);
        drawOverlays(width, height);
        drawFooter(width);
    }

    @Override
    public void resize(int width, int height) {
        uiMatrix.setToOrtho2D(0f, 0f, width, height);
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (shapes != null) {
            shapes.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        if (inputProcessorAttached) {
            Gdx.input.setInputProcessor(null);
            inputProcessorAttached = false;
        }
        for (Texture texture : paletteTextures) {
            texture.dispose();
        }
    }

    private void loadTileset() {
        paletteTextures.clear();
        paletteRegions.clear();
        TilesetData data = TilesetIO.loadFromTsx(tsxPath);
        if (!data.isEmpty()) {
            paletteTextures.addAll(data.textures());
            paletteRegions.addAll(data.regions());
            paletteCols = Math.max(1, data.columns());
            paletteRows = Math.max(1, data.rows());
            Gdx.app.log("PaletteWindow", "Loaded " + paletteRegions.size() + " tiles from " + tsxPath);
            return;
        }
        loadPaletteFromFallbackDirectory();
        if (paletteRegions.isEmpty()) {
            throw new IllegalStateException("Palette window could not load tileset.");
        }
    }

    private void loadPaletteFromFallbackDirectory() {
        FileHandle directory = Gdx.files.internal(fallbackDirectory);
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            Gdx.app.error("PaletteWindow", "Fallback directory missing: " + fallbackDirectory);
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
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            paletteTextures.add(texture);
            paletteRegions.add(new TextureRegion(texture));
            loaded++;
        }
        if (loaded > 0) {
            paletteCols = Math.max(1, (int) Math.ceil(Math.sqrt(paletteRegions.size())));
            paletteRows = (int) Math.ceil(paletteRegions.size() / (float) paletteCols);
            Gdx.app.log("PaletteWindow", "Loaded " + paletteRegions.size() + " PNGs from " + fallbackDirectory);
        }
    }

    private void handleScrollInput(int height) {
        if (pendingScroll != 0f) {
            float scrollDelta = pendingScroll * tileSize * 0.35f;
            pendingScroll = 0f;
            scrollOffset = MathUtils.clamp(scrollOffset + scrollDelta, 0f, maxScroll(height));
        }
        if (Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) {
            scrollOffset = MathUtils.clamp(scrollOffset - tileSize * 0.5f, 0f, maxScroll(height));
        } else if (Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) {
            scrollOffset = MathUtils.clamp(scrollOffset + tileSize * 0.5f, 0f, maxScroll(height));
        }
    }

    private void handleClickInput() {
        if (hoverIndex >= 0 && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            sharedState.requestSelection(hoverIndex);
        }
    }

    private void updateGridLayout(int width) {
        if (paletteRegions.isEmpty()) {
            paletteCols = paletteRows = 0;
            return;
        }
        float usableWidth = Math.max(tileSize, width - 2f * PADDING);
        int columns = Math.max(1, (int) Math.floor(usableWidth / tileSize));
        columns = Math.min(columns, Math.max(1, paletteRegions.size()));
        paletteCols = columns;
        paletteRows = (int) Math.ceil(paletteRegions.size() / (float) paletteCols);
    }

    private void clampScroll(int height) {
        scrollOffset = MathUtils.clamp(scrollOffset, 0f, maxScroll(height));
    }

    private float maxScroll(int height) {
        float availableHeight = Math.max(tileSize, height - HEADER_HEIGHT - PADDING * 2f);
        float contentHeight = paletteRows * tileSize;
        if (contentHeight <= availableHeight) {
            return 0f;
        }
        return contentHeight - availableHeight;
    }

    private void updateHover(int width, int height) {
        if (paletteRegions.isEmpty() || paletteCols <= 0) {
            hoverIndex = -1;
            return;
        }
        int screenX = Gdx.input.getX();
        int screenY = Gdx.graphics.getHeight() - Gdx.input.getY();
        float x = screenX - PADDING;
        if (x < 0f) {
            hoverIndex = -1;
            return;
        }
        int col = (int) Math.floor(x / tileSize);
        if (col < 0 || col >= paletteCols) {
            hoverIndex = -1;
            return;
        }
        float contentTop = contentTop(height);
        float distanceFromTop = (contentTop + scrollOffset) - screenY;
        if (distanceFromTop < 0f) {
            hoverIndex = -1;
            return;
        }
        int row = (int) Math.floor(distanceFromTop / tileSize);
        if (row < 0 || row >= paletteRows) {
            hoverIndex = -1;
            return;
        }
        int index = row * paletteCols + col;
        hoverIndex = index >= paletteRegions.size() ? -1 : index;
    }

    private void drawTiles(int width, int height) {
        batch.begin();
        float contentTop = contentTop(height);
        for (int index = 0; index < paletteRegions.size(); index++) {
            int row = index / paletteCols;
            int col = index % paletteCols;
            float drawX = PADDING + col * tileSize;
            float drawY = contentTop - (row + 1) * tileSize + scrollOffset;
            batch.draw(paletteRegions.get(index), drawX, drawY, tileSize, tileSize);
        }
        batch.end();
    }

    private void drawOverlays(int width, int height) {
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.8f, 0.8f, 0.85f, 0.9f);
        float contentTop = contentTop(height);
        float contentBottom = PADDING;
        shapes.rect(PADDING - 1f, contentBottom - 1f,
                paletteCols * tileSize + 2f,
                contentTop - contentBottom + scrollOffset + 2f);
        int selection = sharedState.currentSelection();
        if (selection >= 0) {
            drawTileOutline(selection, 0.1f, 0.6f, 0.95f, height);
        }
        if (hoverIndex >= 0) {
            drawTileOutline(hoverIndex, 0.9f, 0.3f, 0.3f, height);
        }
        shapes.end();
    }

    private void drawTileOutline(int index, float r, float g, float b, int height) {
        if (index < 0 || index >= paletteRegions.size()) {
            return;
        }
        int row = index / paletteCols;
        int col = index % paletteCols;
        float contentTop = contentTop(height);
        float x = PADDING + col * tileSize;
        float y = contentTop - (row + 1) * tileSize + scrollOffset;
        shapes.setColor(r, g, b, 1f);
        shapes.rect(x - 1f, y - 1f, tileSize + 2f, tileSize + 2f);
    }

    private void drawHeader(int width, int height) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.08f);
        shapes.rect(0f, height - HEADER_HEIGHT, width, HEADER_HEIGHT);
        shapes.end();

        batch.begin();
        font.draw(batch, "Tileset Palette", 16f, height - 16f);
        font.draw(batch,
                "Scroll or PageUp/PageDown to browse, click to pick a tile.",
                16f, height - 36f);
        batch.end();
    }

    private void drawFooter(int width) {
        batch.begin();
        int selection = sharedState.currentSelection();
        String selectionInfo = selection < 0 ? "Brush: air" : "Brush: " + describeTile(selection);
        font.draw(batch, selectionInfo, 16f, 24f);
        if (hoverIndex >= 0) {
            font.draw(batch, "Hover: " + describeTile(hoverIndex), width * 0.45f, 24f);
        }
        batch.end();
    }

    private String describeTile(int index) {
        if (index < 0) {
            return "air";
        }
        return "#" + index + " (row " + (index / paletteCols) + ", col " + (index % paletteCols) + ")";
    }

    private float contentTop(int height) {
        return height - HEADER_HEIGHT - PADDING;
    }
}

