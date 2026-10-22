package dev.vanta.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Loads {@code dev.vanta.*} client classes inside the running game.
 *
 * <p>They were compiled against the Yarn named Minecraft jar, so every class is
 * remapped on the way in and defined here; the parent loader provides the obfuscated
 * game classes. This loader is the whole reason the client can call
 * {@code MinecraftClient.getInstance()} without a mod loader present.
 */
public final class ClientLoader extends ClassLoader {

    private static final String[] PARENT_PACKAGES = {"dev.vanta.agent.", "dev.vanta.injector."};

    private final Mappings mappings;
    private final Map<String, byte[]> bytecode = new HashMap<>();
    private int remapped;

    public ClientLoader(ClassLoader gameLoader, Mappings mappings) throws IOException {
        super(gameLoader);
        this.mappings = mappings;
        scan();
    }

    public int classCount() {
        return bytecode.size();
    }

    public int remappedCount() {
        return remapped;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        for (String prefix : PARENT_PACKAGES) {
            if (name.startsWith(prefix)) {
                return super.loadClass(name, resolve);
            }
        }
        byte[] raw = bytecode.get(name.replace('.', '/'));
        if (raw == null) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = findClass(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String internal = name.replace('.', '/');
        byte[] raw = bytecode.get(internal);
        if (raw == null) {
            throw new ClassNotFoundException(name);
        }
        try {
            ClassReader reader = new ClassReader(raw);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassRemapper(writer, new VantaRemapper(mappings)), 0);
            byte[] out = writer.toByteArray();
            remapped++;
            return defineClass(name, out, 0, out.length);
        } catch (Throwable throwable) {
            throw new ClassNotFoundException("could not remap " + name, throwable);
        }
    }

    /** Reads the client classes out of the jar this agent was loaded from. */
    private void scan() throws IOException {
        Path source;
        try {
            URI uri = Mappings.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            source = Path.of(uri);
        } catch (Exception exception) {
            throw new IOException("cannot locate the vanta jar", exception);
        }

        if (Files.isDirectory(source)) {
            try (Stream<Path> walk = Files.walk(source)) {
                walk.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                    String name = source.relativize(path).toString().replace('\\', '/');
                    if (accept(name)) {
                        try {
                            add(name, Files.readAllBytes(path));
                        } catch (IOException ignored) {
                            // A missing class surfaces later as a clear CNFE.
                        }
                    }
                });
            }
            return;
        }

        try (JarFile jar = new JarFile(source.toFile())) {
            for (JarEntry entry : jar.stream().toList()) {
                String name = entry.getName();
                if (!accept(name)) {
                    continue;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    add(name, in.readAllBytes());
                }
            }
        }
    }

    private static boolean accept(String name) {
        if (!name.startsWith("dev/vanta/") || !name.endsWith(".class")) {
            return false;
        }
        for (String prefix : PARENT_PACKAGES) {
            if (name.startsWith(prefix.replace('.', '/'))) {
                return false;
            }
        }
        return !name.contains("module-info");
    }

    private void add(String name, byte[] bytes) {
        String internal = name.substring(0, name.length() - ".class".length());
        bytecode.put(internal, bytes);
        ClassReader reader = new ClassReader(bytes);
        String superName = reader.getSuperName();
        mappings.addOwn(internal, superName == null ? "" : superName, reader.getInterfaces());
    }
}
