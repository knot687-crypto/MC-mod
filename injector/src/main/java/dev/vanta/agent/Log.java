package dev.vanta.agent;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Every line goes to stderr and to the log file set by the agent arguments. */
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
        StringWriter trace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(trace));
        for (String line : trace.toString().split("\\R")) {
            write("ERROR", "  " + line);
        }
    }

    private static void write(String level, String message) {
        String line = "[" + LocalTime.now().format(TIME) + "] [" + level + "] " + message;
        System.err.println("[vanta] " + line);
        PrintStream sink = file;
        if (sink != null) {
            sink.println(line);
        }
    }
}
