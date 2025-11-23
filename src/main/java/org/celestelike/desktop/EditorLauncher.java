package org.celestelike.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.celestelike.tools.editor.MapEditorApp;

/**
 * Desktop launcher for the in-engine map editor.
 *
 * <p>Optional arguments:
 * <ul>
 *   <li>args[0] – TSX path to load (e.g. "assets/b.tsx"). Defaults to the runtime tileset.</li>
 * </ul>
 */
public final class EditorLauncher {

    private EditorLauncher() {
    }

    public static void main(String[] args) {
        String tsxPath = args != null && args.length > 0
                ? args[0]
                : "assets/b.tsx";

        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Celeste-like Map Editor");
        config.setWindowedMode(1600, 900);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setResizable(true);

        new Lwjgl3Application(new MapEditorApp(tsxPath), config);
    }
}


