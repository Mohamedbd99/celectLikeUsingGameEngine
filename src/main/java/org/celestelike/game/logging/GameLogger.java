package org.celestelike.game.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralized file logger for gameplay events.
 */
public final class GameLogger {

    private static final Path LOG_PATH = Paths.get("logs", "game.log");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        try {
            Files.createDirectories(LOG_PATH.getParent());
            if (!Files.exists(LOG_PATH)) {
                Files.createFile(LOG_PATH);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private GameLogger() {}

    private static synchronized void write(String category, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String line = "[" + timestamp + "] [" + category + "] " + message;
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
                LOG_PATH,
                StandardOpenOption.APPEND))) {
            writer.println(line);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void info(String message) {
        write("INFO", message);
    }

    public static void stateTransition(String target, String from, String to) {
        write("STATE", target + ": " + from + " -> " + to);
    }

    public static void decoratorApplied(String decorator, String target, String duration) {
        write("DECORATOR", decorator + " applied to " + target + (duration == null ? "" : " (duration=" + duration + ")"));
    }

    public static void decoratorRemoved(String decorator, String target) {
        write("DECORATOR", decorator + " removed from " + target);
    }

    public static void entityCreated(String type, String identifier) {
        write("ENTITY", "CREATED " + type + " [" + identifier + "]");
    }

    public static void entityDestroyed(String type, String identifier) {
        write("ENTITY", "DESTROYED " + type + " [" + identifier + "]");
    }

    public static void collision(String description) {
        write("COLLISION", description);
    }

    public static void levelChange(String from, String to) {
        write("LEVEL", from + " -> " + to);
    }
}


