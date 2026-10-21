package dev.vanta.injector;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Plain logger; every line goes to stderr and to vanta-inject.log when a file is set. */
public final class Log {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static PrintStream file;

    private Log() {
    }

    public static void toFile(String path) {
        try {
            file = new PrintStream(new FileOutputStream(path, true), true, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            file = null;
        }
    }

    public static void info(String message) {
        write("INFO", message);
    }

    public static void warn(String message) {
        write("WARN", message);
    }

    public static void error(String message) {
        write("ERROR", message);
    }

    public static void error(String message, Throwable throwable) {
        write("ERROR", message + " :: " + throwable);
        Throwable cause = throwable.getCause();
        while (cause != null) {
            write("ERROR", "  caused by " + cause);
            cause = cause.getCause();
        }
    }

    private static void write(String level, String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] [" + level + "] " + message;
        System.err.println("[vanta] " + line);
        if (file != null) {
            file.println(line);
        }
    }
}
