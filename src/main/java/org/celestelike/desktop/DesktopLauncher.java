package org.celestelike.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.celestelike.game.CelesteGame;

/**
 * Desktop launcher for the game
 * Configured for smooth 60 FPS gameplay
 */
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        
        // Set window title
        config.setTitle("Celeste-like Platformer");
        
        // Set window size (can be adjusted)
        config.setWindowedMode(1280, 720);
        
        // Enable VSync for smooth frame pacing
        config.useVsync(true);
        
        // Set foreground FPS target (60 FPS)
        config.setForegroundFPS(60);
        
        // Optional: Set window icon
        // config.setWindowIcon("icon.png");
        
        // Optional: Disable window resizing for consistent gameplay
        config.setResizable(false);
        
        // Optional: Set fullscreen
       //  //   config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        
        // Launch the core game (viewer only)
        new Lwjgl3Application(new CelesteGame(), config);
    }
}

