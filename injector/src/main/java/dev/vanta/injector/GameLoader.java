package dev.vanta.injector;

import java.lang.management.ManagementFactory;
import java.util.Map;

/** Finds the class loader the game actually runs on. */
final class GameLoader {

    private GameLoader() {
    }

    static ClassLoader find() throws Exception {
        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();

        for (Thread thread : threads.keySet()) {
            if ("Render thread".equals(thread.getName())) {
                ClassLoader loader = thread.getContextClassLoader();
                if (loader != null) {
                    return loader;
                }
            }
        }
        for (Thread thread : threads.keySet()) {
            String name = thread.getName();
            if (name.contains("Render") || name.contains("Client")) {
                ClassLoader loader = thread.getContextClassLoader();
                if (loader != null) {
                    return loader;
                }
            }
        }

        // Last resort: the loader that owns the game jar referenced on the command line.
        for (String argument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (argument.contains("java.class.path")) {
                break;
            }
        }
        ClassLoader system = ClassLoader.getSystemClassLoader();
        Log.warn("render thread not found, falling back to the system loader");
        return system;
    }
}
