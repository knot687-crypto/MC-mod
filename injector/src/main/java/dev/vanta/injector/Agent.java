package dev.vanta.injector;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Java agent entry point.
 *
 * <p>Works both ways: {@code -javaagent:vanta-injector.jar=client=...} at launch, and
 * {@code VirtualMachine.loadAgent} into an already running instance. Premain and
 * agentmain share the same path so behaviour cannot drift between them.
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
        Map<String, String> options = parse(arguments);
        Log.toFile(options.getOrDefault("log", "vanta-inject.log"));
        Log.info("agent attached, canRetransform=" + instrumentation.isRetransformClassesSupported());

        String clientPath = options.get("client");
        if (clientPath == null || clientPath.isBlank()) {
            Log.error("no client jar given; pass client=<path to vanta-client.jar>");
            return;
        }
        Path clientJar = Path.of(clientPath).toAbsolutePath();
        if (!clientJar.toFile().isFile()) {
            Log.error("client jar not found: " + clientJar);
            return;
        }

        try {
            ClassLoader gameLoader = GameLoader.find();
            Log.info("game loader: " + gameLoader);

            URLClassLoader clientLoader = new URLClassLoader(
                    new URL[]{clientJar.toUri().toURL()}, gameLoader);
            Class<?> entry = Class.forName("dev.vanta.agent.Injected", true, clientLoader);
            entry.getMethod("start", ClassLoader.class).invoke(null, clientLoader);
            Log.info("client loaded from " + clientJar.getFileName());

            Resolver resolver = Resolver.create(gameLoader);
            new Hooks(instrumentation, gameLoader, resolver).install();

            injected = true;
            Log.info("vanta injected");
        } catch (Throwable throwable) {
            Log.error("injection failed", throwable);
            if (throwable.getCause() instanceof java.lang.reflect.InvocationTargetException) {
                // Already unwrapped by the logger.
            }
        }
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

    static File agentJar() {
        try {
            return new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception exception) {
            return null;
        }
    }
}
