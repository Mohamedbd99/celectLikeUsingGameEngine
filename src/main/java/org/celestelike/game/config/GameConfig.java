package org.celestelike.game.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Minimal runtime configuration loaded from {@code assets/game_config.json}.
 * Lets designers tweak camera/framing and player collision tuning without recompiling.
 */
public final class GameConfig {

    private static final String CONFIG_PATH = "assets/game_config.json";

    private static final float DEFAULT_TILES_WIDE = 28f;
    private static final float DEFAULT_TILES_TALL = 16f;
    private static final float DEFAULT_CAMERA_ZOOM = 1f;

    private static final float DEFAULT_COLLIDER_WIDTH = 39f;
    private static final float DEFAULT_COLLIDER_HEIGHT = 84f;
    private static final float DEFAULT_COLLIDER_OFFSET_X = 27f;
    private static final float DEFAULT_COLLIDER_OFFSET_Y = 0f;
    private static final float DEFAULT_RENDER_OFFSET_X = 0f;
    private static final float DEFAULT_RENDER_OFFSET_Y = -20f;

    private final float cameraTilesWide;
    private final float cameraTilesTall;
    private final float cameraZoom;
    private final PlayerConfig player;

    private GameConfig(
            float cameraTilesWide,
            float cameraTilesTall,
            float cameraZoom,
            PlayerConfig player) {
        this.cameraTilesWide = cameraTilesWide <= 0f ? DEFAULT_TILES_WIDE : cameraTilesWide;
        this.cameraTilesTall = cameraTilesTall <= 0f ? DEFAULT_TILES_TALL : cameraTilesTall;
        this.cameraZoom = cameraZoom <= 0f ? DEFAULT_CAMERA_ZOOM : cameraZoom;
        this.player = player == null ? PlayerConfig.defaults() : player;
    }

    public float cameraTilesWide() {
        return cameraTilesWide;
    }

    public float cameraTilesTall() {
        return cameraTilesTall;
    }

    public float cameraZoom() {
        return cameraZoom;
    }

    public PlayerConfig player() {
        return player;
    }

    public static GameConfig load() {
        if (Gdx.files == null) {
            return defaults();
        }
        FileHandle handle = Gdx.files.internal(CONFIG_PATH);
        if (!handle.exists()) {
            log("GameConfig", "Missing " + CONFIG_PATH + "; using defaults");
            return defaults();
        }
        try {
            JsonValue root = new JsonReader().parse(handle);
            JsonValue camera = root.get("camera");
            float tilesWide = camera == null ? DEFAULT_TILES_WIDE : camera.getFloat("tilesWide", DEFAULT_TILES_WIDE);
            float tilesTall = camera == null ? DEFAULT_TILES_TALL : camera.getFloat("tilesTall", DEFAULT_TILES_TALL);
            float zoom = camera == null ? DEFAULT_CAMERA_ZOOM : camera.getFloat("zoom", DEFAULT_CAMERA_ZOOM);

            PlayerConfig player = parsePlayer(root.get("player"));

            return new GameConfig(tilesWide, tilesTall, zoom, player);
        } catch (Exception exception) {
            logError("GameConfig", "Failed to parse " + CONFIG_PATH, exception);
            return defaults();
        }
    }

    private static PlayerConfig parsePlayer(JsonValue node) {
        if (node == null) {
            return PlayerConfig.defaults();
        }
        float colliderWidth = node.getFloat("colliderWidth", DEFAULT_COLLIDER_WIDTH);
        float colliderHeight = node.getFloat("colliderHeight", DEFAULT_COLLIDER_HEIGHT);
        float colliderOffsetX = node.getFloat("colliderOffsetX", DEFAULT_COLLIDER_OFFSET_X);
        float colliderOffsetY = node.getFloat("colliderOffsetY", DEFAULT_COLLIDER_OFFSET_Y);
        float renderOffsetX = node.getFloat("renderOffsetX", DEFAULT_RENDER_OFFSET_X);
        float renderOffsetY = node.getFloat("renderOffsetY", DEFAULT_RENDER_OFFSET_Y);
        return new PlayerConfig(
                colliderWidth,
                colliderHeight,
                colliderOffsetX,
                colliderOffsetY,
                renderOffsetX,
                renderOffsetY);
    }

    private static GameConfig defaults() {
        return new GameConfig(DEFAULT_TILES_WIDE, DEFAULT_TILES_TALL, DEFAULT_CAMERA_ZOOM, PlayerConfig.defaults());
    }

    private static void log(String tag, String message) {
        if (Gdx.app != null) {
            Gdx.app.log(tag, message);
        }
    }

    private static void logError(String tag, String message, Exception exception) {
        if (Gdx.app != null) {
            Gdx.app.error(tag, message, exception);
        } else if (exception != null) {
            exception.printStackTrace();
        }
    }

    public static final class PlayerConfig {
        private final float colliderWidth;
        private final float colliderHeight;
        private final float colliderOffsetX;
        private final float colliderOffsetY;
        private final float renderOffsetX;
        private final float renderOffsetY;

        private PlayerConfig(
                float colliderWidth,
                float colliderHeight,
                float colliderOffsetX,
                float colliderOffsetY,
                float renderOffsetX,
                float renderOffsetY) {
            this.colliderWidth = colliderWidth <= 0f ? DEFAULT_COLLIDER_WIDTH : colliderWidth;
            this.colliderHeight = colliderHeight <= 0f ? DEFAULT_COLLIDER_HEIGHT : colliderHeight;
            this.colliderOffsetX = colliderOffsetX;
            this.colliderOffsetY = colliderOffsetY;
            this.renderOffsetX = renderOffsetX;
            this.renderOffsetY = renderOffsetY;
        }

        public float colliderWidth() {
            return colliderWidth;
        }

        public float colliderHeight() {
            return colliderHeight;
        }

        public float colliderOffsetX() {
            return colliderOffsetX;
        }

        public float colliderOffsetY() {
            return colliderOffsetY;
        }

        public float renderOffsetX() {
            return renderOffsetX;
        }

        public float renderOffsetY() {
            return renderOffsetY;
        }

        private static PlayerConfig defaults() {
            return new PlayerConfig(
                    DEFAULT_COLLIDER_WIDTH,
                    DEFAULT_COLLIDER_HEIGHT,
                    DEFAULT_COLLIDER_OFFSET_X,
                    DEFAULT_COLLIDER_OFFSET_Y,
                    DEFAULT_RENDER_OFFSET_X,
                    DEFAULT_RENDER_OFFSET_Y);
        }
    }
}


