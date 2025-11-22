package org.celestelike.game.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.XmlReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility loader that keeps TSX parsing logic in one place so the runtime,
 * editor, and inspector stay in sync when the tileset changes.
 */
public final class TilesetIO {

    private TilesetIO() {
    }

    /**
     * Attempts to load a TSX tileset relative to {@link Gdx#files}.
     *
     * @param tsxPath internal path (e.g. "assets/b.tsx")
     * @return parsed tileset data; {@link TilesetData#isEmpty()} will be true if the file
     *         was missing or could not be parsed.
     */
    public static TilesetData loadFromTsx(String tsxPath) {
        FileHandle handle = Gdx.files.internal(tsxPath);
        if (handle == null || !handle.exists()) {
            Gdx.app.log("TilesetIO", "TSX missing: " + tsxPath);
            return TilesetData.empty();
        }
        return loadFromTsx(handle);
    }

    /**
     * Parses a TSX file and slices the referenced image into {@link TextureRegion}s.
     */
    public static TilesetData loadFromTsx(FileHandle tsxHandle) {
        List<Texture> textures = new ArrayList<>();
        List<TextureRegion> regions = new ArrayList<>();
        try {
            XmlReader.Element root = new XmlReader().parse(tsxHandle);
            int tileWidth = root.getIntAttribute("tilewidth");
            int tileHeight = root.getIntAttribute("tileheight");
            int spacing = root.getIntAttribute("spacing", 0);
            int margin = root.getIntAttribute("margin", 0);
            int tileCount = root.getIntAttribute("tilecount", -1);
            int columns = root.getIntAttribute("columns", -1);

            XmlReader.Element imageElement = root.getChildByName("image");
            if (imageElement == null) {
                Gdx.app.error("TilesetIO", "TSX missing <image>: " + tsxHandle.path());
                disposeTextures(textures);
                return TilesetData.empty();
            }
            String source = imageElement.getAttribute("source");
            FileHandle imageHandle = tsxHandle.parent().child(source);
            if (!imageHandle.exists()) {
                Gdx.app.error("TilesetIO", "Tileset image missing: " + imageHandle.path());
                disposeTextures(textures);
                return TilesetData.empty();
            }

            Texture texture = new Texture(imageHandle);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            textures.add(texture);

            int imageWidth = texture.getWidth();
            int imageHeight = texture.getHeight();
            if (columns <= 0) {
                int availableWidth = imageWidth - margin * 2 + spacing;
                columns = Math.max(1, availableWidth / (tileWidth + spacing));
            }
            int rowsVisible = Math.max(1,
                    (imageHeight - margin * 2 + spacing) / (tileHeight + spacing));
            if (tileCount <= 0) {
                tileCount = columns * rowsVisible;
            }

            int produced = 0;
            outer:
            for (int row = 0; row < rowsVisible; row++) {
                for (int col = 0; col < columns; col++) {
                    if (produced >= tileCount) {
                        break outer;
                    }
                    int x = margin + col * (tileWidth + spacing);
                    int y = margin + row * (tileHeight + spacing);
                    if (x + tileWidth > imageWidth || y + tileHeight > imageHeight) {
                        break outer;
                    }
                    TextureRegion region = new TextureRegion(texture, x, y, tileWidth, tileHeight);
                    regions.add(region);
                    produced++;
                }
            }

            int paletteRows = columns == 0 ? 0 : (int) Math.ceil(produced / (float) columns);
            Gdx.app.log("TilesetIO",
                    "Loaded %d tiles from %s (%dx%d)".formatted(produced, tsxHandle.name(), columns, paletteRows));
            return new TilesetData(textures, regions, columns, paletteRows);
        } catch (Exception exception) {
            Gdx.app.error("TilesetIO", "Failed to parse TSX " + tsxHandle.path(), exception);
            disposeTextures(textures);
            return TilesetData.empty();
        }
    }

    private static void disposeTextures(List<Texture> textures) {
        for (Texture texture : textures) {
            texture.dispose();
        }
        textures.clear();
    }

    /**
     * Immutable result of slicing a tileset. The caller owns the textures and must dispose them.
     */
    public record TilesetData(List<Texture> textures, List<TextureRegion> regions, int columns, int rows) {

        public static TilesetData empty() {
            return new TilesetData(new ArrayList<>(), new ArrayList<>(), 0, 0);
        }

        public boolean isEmpty() {
            return regions == null || regions.isEmpty();
        }
    }
}

