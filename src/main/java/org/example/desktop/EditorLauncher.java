package org.example.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.example.ViewEditor;

/**
 * Stand-alone launcher for the in-game editor.
 * Run this instead of {@link DesktopLauncher} when you want to author maps.
 */
public final class EditorLauncher {

    private EditorLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Celeste-like Editor");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new ViewEditor(), config);
    }
}

