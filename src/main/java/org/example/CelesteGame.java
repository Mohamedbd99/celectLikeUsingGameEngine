package org.example;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.LevelData.TileBlueprint;

/**
 * Tile editor for the Kenney Pico-8 tileset that lets you pick any sprite and
 * configure simple horizontal animations directly inside the running game.
 */
public class CelesteGame extends ApplicationAdapter {

    private static final String KENNEY_BASE = "assets/kenney_pico-8-platformer/";
    private static final float TILE_SCALE = 4f;          // 8px -> 32px world units
    private static final float PALETTE_SCALE = 2f;       // palette tiles drawn at 16px
    private static final float PALETTE_MARGIN = 16f;
    private static final float PALETTE_HEADER_HEIGHT = 28f;
    private static final float MIN_FRAME_DURATION = 0.05f;
    private static final float MAX_FRAME_DURATION = 0.5f;
    private static final float FRAME_DURATION_STEP = 0.02f;

    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private final List<Texture> paletteTextures = new ArrayList<>();
    private final List<TextureRegion> paletteRegions = new ArrayList<>();
    private int paletteRows;
    private int paletteCols = LevelData.ATLAS_COLUMNS;

    private TileCell[][] cells;
    private float tileWorldSize;
    private float worldWidth;
    private float worldHeight;
    private float elapsed;

    private int hoverRow = -1;
    private int hoverCol = -1;
    private boolean pointerOverPalette;
    private int paletteHoverIndex = -1;
    private boolean draggingPalette;
    private float dragOffsetX;
    private float dragOffsetY;

    private int selectedTileIndex = -1;
    private int selectedFrameCount = 1;
    private float selectedFrameDuration = 0.15f;

    private final Vector2 tmpWorld = new Vector2();
    private final Rectangle paletteBounds = new Rectangle();
    private final Matrix4 uiMatrix = new Matrix4();

    @Override
    public void create() {
        tileWorldSize = LevelData.TILE_SIZE * TILE_SCALE;

        TileBlueprint[][] blueprint = LevelData.copyBlueprint();
        int rows = blueprint.length;
        int cols = blueprint[0].length;
        worldWidth = cols * tileWorldSize;
        worldHeight = rows * tileWorldSize;

        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(worldWidth, worldHeight, worldCamera);
        worldViewport.apply();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.05f);
        Gdx.graphics.setVSync(true);

        uiMatrix.setToOrtho2D(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        loadTileset();
        loadInitialCells(blueprint);
        if (!paletteRegions.isEmpty()) {
            int defaultIndex = Math.min(atlasIndex(6, 1), paletteRegions.size() - 1);
            setSelectedTileIndex(defaultIndex);
        } else {
            setSelectedTileIndex(-1);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        elapsed += delta;

        updateHoverState();
        handleEditorInput();

        ScreenUtils.clear(0.08f, 0.08f, 0.12f, 1f);

        worldCamera.update();
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        renderTerrain();
        batch.end();

        renderWorldHighlight();
        renderPaletteOverlay();
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, true);
        uiMatrix.setToOrtho2D(0f, 0f, width, height);
        updatePaletteBounds(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
        for (Texture texture : paletteTextures) {
            texture.dispose();
        }
    }

    private void loadTileset() {
        loadPaletteFromTilesDirectory();
        if (paletteRegions.isEmpty()) {
            loadPaletteFromAtlas();
        }
        paletteRows = (int) Math.ceil(Math.max(1, paletteRegions.size()) / (float) paletteCols);
        updatePaletteBounds(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
            consecutiveMisses = 0;
            any = true;
            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            paletteTextures.add(texture);
            paletteRegions.add(new TextureRegion(texture));
        }

        if (any) {
            Gdx.app.log("Palette", "Loaded " + paletteRegions.size() + " sprites from Transparent/Tiles");
        } else {
            Gdx.app.log("Palette", "Could not enumerate Transparent/Tiles; will fall back to atlas");
        }
    }

    private void loadPaletteFromAtlas() {
        try {
            Texture atlas = new Texture(Gdx.files.internal(KENNEY_BASE + "Transparent/Tilemap/tilemap.png"));
            atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            TextureRegion[][] grid = TextureRegion.split(atlas, LevelData.TILE_SIZE, LevelData.TILE_SIZE);
            paletteCols = grid[0].length;
            paletteRows = grid.length;
            for (TextureRegion[] row : grid) {
                paletteRegions.addAll(Arrays.asList(row));
            }
            paletteTextures.add(atlas);
            Gdx.app.log("Palette", "Fallback to tilemap atlas (" + paletteRegions.size() + " tiles)");
        } catch (Exception exception) {
            Gdx.app.error("Palette", "Failed to load fallback tilemap atlas", exception);
        }
    }

    private void loadInitialCells(TileBlueprint[][] blueprint) {
        int rows = blueprint.length;
        int cols = blueprint[0].length;
        cells = new TileCell[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileBlueprint bp = blueprint[row][col];
                cells[row][col] = TileCell.fromBlueprint(bp);
            }
        }
    }

    private void renderTerrain() {
        int rows = cells.length;
        int cols = cells[0].length;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                TileCell cell = cells[row][col];
                TextureRegion region = cell.getFrame(paletteRegions, elapsed);
                if (region == null) {
                    continue;
                }
                float x = col * tileWorldSize;
                float y = (rows - row - 1) * tileWorldSize;
                batch.draw(region, x, y, tileWorldSize, tileWorldSize);
            }
        }
    }

    private void updateHoverState() {
        int screenX = Gdx.input.getX();
        int screenY = Gdx.graphics.getHeight() - Gdx.input.getY();
        pointerOverPalette = paletteBounds.contains(screenX, screenY);
        paletteHoverIndex = pointerOverPalette ? computePaletteIndex(screenX, screenY) : -1;

        if (pointerOverPalette) {
            hoverRow = -1;
            hoverCol = -1;
            return;
        }

        tmpWorld.set(Gdx.input.getX(), Gdx.input.getY());
        worldViewport.unproject(tmpWorld);

        int cols = cells[0].length;
        int rows = cells.length;
        int col = (int) Math.floor(tmpWorld.x / tileWorldSize);
        int rowFromBottom = (int) Math.floor(tmpWorld.y / tileWorldSize);
        int row = rows - rowFromBottom - 1;

        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            hoverRow = -1;
            hoverCol = -1;
            return;
        }

        hoverRow = row;
        hoverCol = col;
    }

    private void handleEditorInput() {
        int screenX = Gdx.input.getX();
        int screenY = Gdx.graphics.getHeight() - Gdx.input.getY();

        if (pointerOverPalette) {
            if (!draggingPalette && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
                    && isPointerInPaletteHeader(screenX, screenY)) {
                startPaletteDrag(screenX, screenY);
            } else if (!draggingPalette && paletteHoverIndex >= 0 && Gdx.input.justTouched()) {
                setSelectedTileIndex(paletteHoverIndex);
            }
        }

        if (draggingPalette) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                dragPalette(screenX, screenY);
            } else {
                draggingPalette = false;
            }
            return;
        }

        if (!pointerOverPalette && hoverRow >= 0 && hoverCol >= 0) {
            if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                applyBrush(hoverRow, hoverCol);
            } else if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                clearCell(hoverRow, hoverCol);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            setSelectedTileIndex(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT_BRACKET)) {
            adjustFrameCount(-1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT_BRACKET)) {
            adjustFrameCount(1);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT)) {
            adjustFrameDuration(-FRAME_DURATION_STEP);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)
                || Gdx.input.isKeyJustPressed(Input.Keys.PLUS)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD)) {
            adjustFrameDuration(FRAME_DURATION_STEP);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
            appendFrameToHoveredCell();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            dumpTerrainToConsole();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            clearAllTiles();
        }
    }

    private void applyBrush(int row, int col) {
        if (selectedTileIndex < 0) {
            clearCell(row, col);
            return;
        }
        int frames = clampFramesForTile(selectedTileIndex, selectedFrameCount);
        cells[row][col].setContiguousFrames(selectedTileIndex, frames, selectedFrameDuration);
    }

    private void clearCell(int row, int col) {
        cells[row][col].clear(selectedFrameDuration);
    }

    private void setSelectedTileIndex(int tileIndex) {
        if (paletteRegions.isEmpty()) {
            selectedTileIndex = -1;
            selectedFrameCount = 1;
            return;
        }
        if (tileIndex >= paletteRegions.size()) {
            tileIndex = paletteRegions.size() - 1;
        }
        selectedTileIndex = tileIndex;
        selectedFrameCount = clampFramesForTile(tileIndex, selectedFrameCount);
        Gdx.app.log("Palette", tileIndex < 0 ? "Selected: air" : "Selected tile " + describeTile(tileIndex));
    }

    private void adjustFrameCount(int delta) {
        selectedFrameCount = clampFramesForTile(selectedTileIndex, selectedFrameCount + delta);
    }

    private void adjustFrameDuration(float delta) {
        selectedFrameDuration = Math.max(MIN_FRAME_DURATION,
                Math.min(MAX_FRAME_DURATION, selectedFrameDuration + delta));
    }

    private void dumpTerrainToConsole() {
        StringBuilder sb = new StringBuilder();
        sb.append("public static final TileBlueprint[][] CUSTOM_MAP = {\n");
        for (int row = 0; row < cells.length; row++) {
            sb.append("    {");
            for (int col = 0; col < cells[row].length; col++) {
                TileCell cell = cells[row][col];
                sb.append("new TileBlueprint(new int[]{");
                for (int i = 0; i < cell.frameCount; i++) {
                    sb.append(cell.frameIndices[i]);
                    if (i < cell.frameCount - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("}, ")
                        .append(String.format(Locale.US, "%.2ff", cell.frameDuration))
                        .append(")");
                if (col < cells[row].length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            if (row < cells.length - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("};\n");
        System.out.println(sb);
    }

    private void clearAllTiles() {
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[row].length; col++) {
                cells[row][col].clear(selectedFrameDuration);
            }
        }
        Gdx.app.log("Editor", "Cleared map (all air)");
    }

    private void renderWorldHighlight() {
        if (hoverRow < 0 || hoverCol < 0) {
            return;
        }
        int rows = cells.length;
        float x = hoverCol * tileWorldSize;
        float y = (rows - hoverRow - 1) * tileWorldSize;

        shapeRenderer.setProjectionMatrix(worldCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
        shapeRenderer.rect(x, y, tileWorldSize, tileWorldSize);
        shapeRenderer.end();
    }

    private void renderPaletteOverlay() {
        shapeRenderer.setProjectionMatrix(uiMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(paletteBounds.x - 4f, paletteBounds.y - 4f,
                paletteBounds.width + 8f, paletteBounds.height + 8f);
        float headerY = paletteBounds.y + paletteBounds.height - PALETTE_HEADER_HEIGHT;
        shapeRenderer.setColor(0.12f, 0.12f, 0.16f, 0.9f);
        shapeRenderer.rect(paletteBounds.x - 4f, headerY,
                paletteBounds.width + 8f, PALETTE_HEADER_HEIGHT);
        shapeRenderer.end();

        batch.setProjectionMatrix(uiMatrix);
        batch.begin();
        float paletteTileSize = LevelData.TILE_SIZE * PALETTE_SCALE;
        int total = paletteRegions.size();
        for (int idx = 0; idx < total; idx++) {
            float[] rect = paletteRectForIndex(idx, paletteTileSize);
            batch.draw(paletteRegions.get(idx), rect[0], rect[1], rect[2], rect[3]);
        }
        font.draw(batch, "Tile Palette (drag the top bar)",
                paletteBounds.x, headerY + PALETTE_HEADER_HEIGHT - 8f);
        float infoY = paletteBounds.y + paletteBounds.height + 16f;
        String brushInfo = selectedTileIndex < 0 ? "Brush: air" : "Brush: " + describeTile(selectedTileIndex);
        font.draw(batch, brushInfo, paletteBounds.x, infoY);
        font.draw(batch,
                String.format(Locale.US, "Frames: %d  Duration: %.2fs  ([/], -/+, X=air)",
                        selectedFrameCount, selectedFrameDuration),
                paletteBounds.x, infoY - 16f);
        font.draw(batch, "Click palette to pick tile. LMB paint, RMB erase, P=print, C=clear.",
                paletteBounds.x, infoY - 32f);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.9f);
        shapeRenderer.rect(paletteBounds.x - 4f, paletteBounds.y - 4f,
                paletteBounds.width + 8f, paletteBounds.height + 8f);
        if (selectedTileIndex >= 0) {
            drawPaletteRect(selectedTileIndex, 0.95f, 0.85f, 0.2f);
        }
        if (paletteHoverIndex >= 0) {
            drawPaletteRect(paletteHoverIndex, 0.9f, 0.4f, 0.4f);
        }
        shapeRenderer.end();
    }

    private void drawPaletteRect(int tileIndex, float r, float g, float b) {
        float[] rect = paletteRectForIndex(tileIndex, LevelData.TILE_SIZE * PALETTE_SCALE);
        shapeRenderer.setColor(r, g, b, 1f);
        shapeRenderer.rect(rect[0], rect[1], rect[2], rect[3]);
    }

    private int clampFramesForTile(int tileIndex, int desiredFrames) {
        if (tileIndex < 0) {
            return 1;
        }
        int availableFrames = paletteRegions.size() - tileIndex;
        if (availableFrames < 1) {
            availableFrames = 1;
        }
        int maxFrames = Math.min(TileCell.MAX_FRAMES, availableFrames);
        int frames = Math.max(1, desiredFrames);
        return Math.min(frames, maxFrames);
    }

    private void updatePaletteBounds(int width, int height) {
        float paletteTileSize = LevelData.TILE_SIZE * PALETTE_SCALE;
        float gridWidth = paletteCols * paletteTileSize;
        float gridHeight = paletteRows * paletteTileSize;
        float totalWidth = gridWidth;
        float totalHeight = gridHeight + PALETTE_HEADER_HEIGHT;
        if (paletteBounds.width == 0f && paletteBounds.height == 0f) {
            float x = Math.max(0f, width - totalWidth - PALETTE_MARGIN);
            float y = PALETTE_MARGIN;
            paletteBounds.set(x, y, totalWidth, totalHeight);
        } else {
            paletteBounds.width = totalWidth;
            paletteBounds.height = totalHeight;
            float maxX = Math.max(0f, width - totalWidth);
            float maxY = Math.max(0f, height - totalHeight);
            paletteBounds.x = Math.max(0f, Math.min(paletteBounds.x, maxX));
            paletteBounds.y = Math.max(0f, Math.min(paletteBounds.y, maxY));
        }
    }

    private String describeTile(int tileIndex) {
        int row = tileIndex / paletteCols;
        int col = tileIndex % paletteCols;
        return String.format(Locale.US, "row %d col %d (#%d)", row, col, tileIndex);
    }

    private void appendFrameToHoveredCell() {
        if (hoverRow < 0 || hoverCol < 0 || selectedTileIndex < 0 || paletteRegions.isEmpty()) {
            return;
        }
        TileCell cell = cells[hoverRow][hoverCol];
        if (cell.addFrame(selectedTileIndex, selectedFrameDuration)) {
            Gdx.app.log("Editor", "Added frame " + describeTile(selectedTileIndex) + " to cell");
        } else {
            Gdx.app.log("Editor", "Cell already has max frames (" + TileCell.MAX_FRAMES + ")");
        }
    }

    private int atlasIndex(int row, int col) {
        return row * paletteCols + col;
    }

    private boolean isPointerInPaletteHeader(int screenX, int screenY) {
        if (!paletteBounds.contains(screenX, screenY)) {
            return false;
        }
        float localY = screenY - paletteBounds.y;
        return localY >= paletteBounds.height - PALETTE_HEADER_HEIGHT;
    }

    private void startPaletteDrag(int screenX, int screenY) {
        draggingPalette = true;
        dragOffsetX = screenX - paletteBounds.x;
        float topY = paletteBounds.y + paletteBounds.height;
        dragOffsetY = topY - screenY;
    }

    private void dragPalette(int screenX, int screenY) {
        float newX = screenX - dragOffsetX;
        float newTop = screenY + dragOffsetY;
        float newY = newTop - paletteBounds.height;
        float maxX = Math.max(0f, Gdx.graphics.getWidth() - paletteBounds.width);
        float maxY = Math.max(0f, Gdx.graphics.getHeight() - paletteBounds.height);
        paletteBounds.x = Math.max(0f, Math.min(newX, maxX));
        paletteBounds.y = Math.max(0f, Math.min(newY, maxY));
    }

    private int computePaletteIndex(float screenX, float screenY) {
        if (!paletteBounds.contains(screenX, screenY)) {
            return -1;
        }
        float paletteTileSize = LevelData.TILE_SIZE * PALETTE_SCALE;
        float localX = screenX - paletteBounds.x;
        float localY = screenY - paletteBounds.y;
        float usableHeight = paletteRows * paletteTileSize;
        if (localY >= usableHeight) {
            return -1;
        }
        int col = (int) (localX / paletteTileSize);
        int rowFromBottom = (int) (localY / paletteTileSize);
        int row = paletteRows - rowFromBottom - 1;
        if (col < 0 || col >= paletteCols || row < 0 || row >= paletteRows) {
            return -1;
        }
        return row * paletteCols + col;
    }

    private float[] paletteRectForIndex(int tileIndex, float paletteTileSize) {
        int row = tileIndex / paletteCols;
        int col = tileIndex % paletteCols;
        float x = paletteBounds.x + col * paletteTileSize;
        float y = paletteBounds.y + (paletteRows - row - 1) * paletteTileSize;
        return new float[]{x, y, paletteTileSize, paletteTileSize};
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

        void setContiguousFrames(int startIndex, int frames, float duration) {
            frameCount = Math.min(frames, MAX_FRAMES);
            for (int i = 0; i < frameCount; i++) {
                frameIndices[i] = startIndex + i;
            }
            frameDuration = duration;
        }

        void setFrames(int[] frames, float duration) {
            frameCount = Math.min(frames.length, MAX_FRAMES);
            for (int i = 0; i < frameCount; i++) {
                frameIndices[i] = frames[i];
            }
            frameDuration = duration;
        }

        void clear(float duration) {
            frameCount = 0;
            frameDuration = duration;
        }

        boolean addFrame(int tileIndex, float duration) {
            if (frameCount >= MAX_FRAMES) {
                return false;
            }
            if (frameCount < 0 || frameCount >= MAX_FRAMES) {
                Gdx.app.error("TileCell", "Invalid frameCount " + frameCount + " before adding frame");
            }
            frameIndices[frameCount++] = tileIndex;
            frameDuration = duration;
            return true;
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

