package dev.vanta.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 * Java agent entry point. Works from {@code -javaagent:vanta.jar} and from
 * {@code VirtualMachine.loadAgent} into a running game; both paths share one body so
 * behaviour cannot drift.
 */
public final class Agent {

    private static volatile boolean injected;

    private Agent() {
    }

    public static void premain(String arguments, Instrumentation instrumentation) {
        run(arguments, instrumentation);
    }

    public static void agentmain(String arguments, Instrumentation instrumentation) {
        run(arguments, instrumentation);
    }

    private static synchronized void run(String arguments, Instrumentation instrumentation) {
        if (injected) {
            return;
        }
        injected = true;
        Map<String, String> options = parse(arguments);
        Log.toFile(options.getOrDefault("log", "vanta-inject.log"));
        Log.info("attached, retransform=" + instrumentation.isRetransformClassesSupported());

        try {
            Mappings mappings = Mappings.load();
            Log.info("mappings: " + mappings.classCount() + " classes, " + mappings.memberCount() + " members");

            ClassLoader game = gameLoader(instrumentation, mappings);
            Log.info("game loader: " + game);

            ClientLoader loader = new ClientLoader(game, mappings);
            Log.info("client: " + loader.classCount() + " classes staged");

            Hooks.install(instrumentation, mappings, game);

            Class<?> bootstrap = Class.forName("dev.vanta.Bootstrap", true, loader);
            bootstrap.getMethod("start").invoke(null);
            Log.info("vanta is live");
        } catch (Throwable throwable) {
            Log.error("injection failed", throwable);
        }
    }

    /** The loader that owns Minecraft, found by name so it works on any launcher. */
    private static ClassLoader gameLoader(Instrumentation instrumentation, Mappings mappings) {
        String minecraft = mappings.officialClass("net/minecraft/client/MinecraftClient").replace('/', '.');
        for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
            if (loaded.getName().equals(minecraft)) {
                ClassLoader loader = loaded.getClassLoader();
                if (loader != null) {
                    return loader;
                }
            }
        }
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if ("Render thread".equals(thread.getName())) {
                ClassLoader loader = thread.getContextClassLoader();
                if (loader != null) {
                    return loader;
                }
            }
        }
        Log.warn("Minecraft class not loaded yet, using the system loader");
        return ClassLoader.getSystemClassLoader();
    }

    /** Agent arguments arrive as a semicolon separated key=value string. */
    static Map<String, String> parse(String arguments) {
        Map<String, String> options = new HashMap<>();
        if (arguments == null || arguments.isBlank()) {
            return options;
        }
        for (String part : arguments.split(";")) {
            int split = part.indexOf('=');
            if (split <= 0) {
                continue;
            }
            options.put(part.substring(0, split).trim(), part.substring(split + 1).trim());
        }
        return options;
    }
}
