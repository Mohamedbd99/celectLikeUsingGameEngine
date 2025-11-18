package org.example.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.example.inspector.MapInspector;

public class InspectorLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Map Inspector");
        config.setWindowedMode(1280, 720);
        config.useVsync(true);
        new Lwjgl3Application(new MapInspector(), config);
    }
}

