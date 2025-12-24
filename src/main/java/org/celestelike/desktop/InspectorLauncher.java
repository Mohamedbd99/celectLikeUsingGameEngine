package org.celestelike.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.celestelike.tools.inspector.MapInspector;

/**
 * Standalone launcher for the MapInspector tool.
 */
public final class InspectorLauncher {

    private InspectorLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Celeste-like Map Inspector");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new MapInspector(), config);
    }
}

