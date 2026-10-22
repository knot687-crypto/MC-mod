import dev.vanta.agent.Hooks;
import dev.vanta.agent.Mappings;
import dev.vanta.agent.VantaRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Offline proof that the internal actually links against the shipped 1.21.11 jar.
 *
 * <pre>
 *   java -cp vanta-1.0.0.jar tools/Verify.java client-dev.jar minecraft-client.jar
 * </pre>
 *
 * Checks, in order:
 * <ol>
 *   <li>every client class remaps without error,</li>
 *   <li>every remapped class/field/method reference exists in the real game jar,</li>
 *   <li>every method the client overrides resolves to a real parent method,</li>
 *   <li>each hook target exists and its patched bytecode still verifies.</li>
 * </ol>
 */
public final class Verify {

    private static final List<String> failures = new ArrayList<>();
    private static int classRefs;
    private static int memberRefs;

    public static void main(String[] args) throws Exception {
        Path clientJar = Path.of(args[0]);
        Path gameJar = Path.of(args[1]);

        Mappings mappings = Mappings.load();
        System.out.println("mappings: " + mappings.classCount() + " classes, " + mappings.memberCount() + " members");

        Map<String, byte[]> clientClasses = readJar(clientJar);
        for (var entry : clientClasses.entrySet()) {
            ClassReader reader = new ClassReader(entry.getValue());
            String superName = reader.getSuperName();
            mappings.addOwn(reader.getClassName(), superName == null ? "" : superName, reader.getInterfaces());
        }

        VantaRemapper remapper = new VantaRemapper(mappings);
        Map<String, byte[]> remapped = new HashMap<>();
        for (var entry : clientClasses.entrySet()) {
            try {
                ClassReader reader = new ClassReader(entry.getValue());
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, remapper), 0);
                remapped.put(entry.getKey(), writer.toByteArray());
            } catch (Throwable throwable) {
                fail("remap " + entry.getKey() + ": " + throwable);
            }
        }
        System.out.println("remapped " + remapped.size() + "/" + clientClasses.size() + " client classes");

        GameIndex game = new GameIndex(gameJar);
        Set<String> internal = remapped.keySet();

        for (var entry : remapped.entrySet()) {
            checkClass(entry.getKey(), entry.getValue(), game, internal);
        }

        // Overrides must land on a real parent member.
        for (var entry : clientClasses.entrySet()) {
            checkOverrides(entry.getKey(), entry.getValue(), remapped.get(entry.getKey()), mappings);
        }

        // JVM-level: resolve every remapped class so HotSpot verifies the bytecode.
        linkCheck(remapped, gameJar);

        // Hooks: targets resolve, patch, and still verify.
        Hooks.Patcher patcher = new Hooks.Patcher(mappings);
        System.out.println("hooks: " + patcher.descriptions());
        for (String owner : patcher.owners()) {
            byte[] original = game.bytes(owner);
            if (original == null) {
                fail("hook owner missing from the game jar: " + owner);
                continue;
            }
            byte[] patched;
            try {
                patched = patcher.patch(owner, original);
            } catch (Throwable throwable) {
                fail("patch " + owner + ": " + throwable);
                continue;
            }
            if (patched == null) {
                fail("patch produced nothing for " + owner);
                continue;
            }
            checkPatch(owner, original, patched, patcher.ruleCount(owner));
            verifyBytecode(owner, patched, gameJar);
            loadPatched(owner, patched, gameJar);
            System.out.println("  patched + verified " + owner);
        }

        System.out.println("references: " + classRefs + " classes, " + memberRefs + " members");
        if (failures.isEmpty()) {
            System.out.println("RESULT: all checks passed");
        } else {
            System.out.println("RESULT: " + failures.size() + " failure(s)");
            for (String failure : failures) {
                System.out.println("  ! " + failure);
            }
            System.exit(1);
        }
    }

    /** Defines every remapped client class against the real jar; linking verifies the bytecode. */
    private static void linkCheck(Map<String, byte[]> remapped, Path gameJar) throws Exception {
        ClassLoader libraries = libraryLoader(gameJar);
        ClassLoader loader = new ClassLoader(libraries) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                byte[] bytes = remapped.get(name.replace('.', '/'));
                if (bytes == null) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
        };
        int linked = 0;
        for (String name : remapped.keySet()) {
            try {
                loader.loadClass(name.replace('/', '.'));
                linked++;
            } catch (Throwable throwable) {
                fail("link " + name + ": " + throwable);
            }
        }
        System.out.println("linked " + linked + "/" + remapped.size() + " remapped classes in the JVM");
    }

    private static void checkClass(String name, byte[] bytecode, GameIndex game, Set<String> internal) {
        ClassNode node = new ClassNode();
        new ClassReader(bytecode).accept(node, 0);

        checkType(node.superName, game, internal, name + " super");
        for (String iface : node.interfaces) {
            checkType(iface, game, internal, name + " interface");
        }

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call) {
                    checkType(call.owner, game, internal, name + "." + method.name + " -> " + call.owner);
                    if (isGame(call.owner, game, internal)) {
                        memberRefs++;
                    }
                    if (isGame(call.owner, game, internal) && !game.hasMethod(call.owner, call.name, call.desc)) {
                        fail(name + "." + method.name + " calls missing " + call.owner + "#" + call.name + call.desc);
                    }
                } else if (insn instanceof FieldInsnNode field) {
                    checkType(field.owner, game, internal, name + "." + method.name + " -> " + field.owner);
                    if (isGame(field.owner, game, internal)) {
                        memberRefs++;
                    }
                    if (isGame(field.owner, game, internal) && !game.hasField(field.owner, field.name, field.desc)) {
                        fail(name + "." + method.name + " reads missing " + field.owner + "#" + field.name + " " + field.desc);
                    }
                } else if (insn instanceof TypeInsnNode type) {
                    checkType(type.desc, game, internal, name + "." + method.name + " -> " + type.desc);
                }
            }
        }
        for (FieldNode field : node.fields) {
            checkType(field.desc, game, internal, name + "." + field.name);
        }
    }

    /** A reference belongs to the game when the game jar has it, or should have it. */
    private static boolean isGame(String owner, GameIndex game, Set<String> internal) {
        if (owner.startsWith("dev/vanta/") || internal.contains(owner)) {
            return false;
        }
        return game.exists(owner) || owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/");
    }

    /** A class token must be the client itself, a game class, or a plain library class. */
    private static void checkType(String internalName, GameIndex game, Set<String> internal, String origin) {
        if (internalName == null || internalName.isEmpty() || internalName.startsWith("[")) {
            return;
        }
        if (internalName.contains(".")) {
            return;
        }
        if (internal.contains(internalName) || internalName.startsWith("dev/vanta/")) {
            return;
        }
        if (!isGame(internalName, game, internal)) {
            return;
        }
        classRefs++;
        if (!game.exists(internalName)) {
            fail(origin + ": missing class " + internalName);
        }
    }

    private static void checkOverrides(String name, byte[] original, byte[] remapped, Mappings mappings) {
        ClassNode before = new ClassNode();
        ClassNode after = new ClassNode();
        new ClassReader(original).accept(before, 0);
        new ClassReader(remapped).accept(after, 0);

        for (int i = 0; i < before.methods.size(); i++) {
            MethodNode source = before.methods.get(i);
            MethodNode patched = after.methods.get(i);
            if (source.name.startsWith("<")) {
                continue;
            }
            if (mappings.inherited(before.name, true, source.name, source.desc) && patched.name.equals(source.name)) {
                fail(name + "#" + source.name + source.desc + " overrides a parent method but was not remapped");
            }
        }
        for (int i = 0; i < before.fields.size(); i++) {
            FieldNode source = before.fields.get(i);
            FieldNode patched = after.fields.get(i);
            if (mappings.inherited(before.name, false, source.name, source.desc) && patched.name.equals(source.name)) {
                fail(name + "#" + source.name + " shadows a parent field but was not remapped");
            }
        }
    }

    private static void checkPatch(String owner, byte[] original, byte[] patched, int expected) {
        ClassNode before = new ClassNode();
        ClassNode after = new ClassNode();
        new ClassReader(original).accept(before, 0);
        new ClassReader(patched).accept(after, 0);
        if (before.methods.size() != after.methods.size()) {
            fail(owner + ": method count changed");
        }
        int calls = 0;
        for (MethodNode method : after.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call && call.owner.equals("dev/vanta/agent/Bridge")) {
                    calls++;
                }
            }
        }
        if (calls < expected) {
            fail(owner + ": expected at least " + expected + " bridge call(s), found " + calls);
        }
    }

    private static void verifyBytecode(String owner, byte[] patched, Path classpath) {
        try {
            StringWriter output = new StringWriter();
            org.objectweb.asm.util.CheckClassAdapter.verify(
                    new ClassReader(patched),
                    libraryLoader(classpath),
                    false,
                    new PrintWriter(output));
            if (!output.toString().isBlank()) {
                fail(owner + ": verifier reported " + output.toString().lines().findFirst().orElse("an error"));
            }
        } catch (Throwable throwable) {
            fail(owner + ": verification threw " + throwable);
        }
    }

    /** Game jar plus every cached library, so the verifier can link what it inspects. */
    private static ClassLoader libraryLoader(Path gameJar) throws Exception {
        List<java.net.URL> urls = new ArrayList<>();
        urls.add(gameJar.toUri().toURL());
        Path caches = Path.of(System.getProperty("user.home"), ".gradle", "caches", "modules-2", "files-2.1");
        if (Files.isDirectory(caches)) {
            try (var walk = Files.walk(caches, 6)) {
                walk.filter(path -> path.toString().endsWith(".jar")).forEach(path -> {
                    try {
                        urls.add(path.toUri().toURL());
                    } catch (Exception ignored) {
                        // A bad path only means one fewer library on the verify classpath.
                    }
                });
            }
        }
        return new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]), Verify.class.getClassLoader());
    }

    /** Defines the patched class on its own so the real JVM verifier sees it. */
    private static void loadPatched(String owner, byte[] patched, Path gameJar) throws Exception {
        String binary = owner.replace('/', '.');
        ClassLoader libraries = libraryLoader(gameJar);
        ClassLoader loader = new ClassLoader(libraries) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(binary)) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        loaded = defineClass(name, patched, 0, patched.length);
                    }
                    if (resolve) {
                        resolveClass(loaded);
                    }
                    return loaded;
                }
            }
        };
        try {
            Class.forName(binary, false, loader);
        } catch (Throwable throwable) {
            fail(owner + ": JVM rejected the patched class: " + throwable);
        }
    }

    private static void fail(String message) {
        failures.add(message);
    }

    private static Map<String, byte[]> readJar(Path jar) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        try (JarFile file = new JarFile(jar.toFile())) {
            for (JarEntry entry : file.stream().toList()) {
                if (entry.getName().endsWith(".class")) {
                    classes.put(entry.getName().substring(0, entry.getName().length() - 6),
                            file.getInputStream(entry).readAllBytes());
                }
            }
        }
        return classes;
    }

    /** Lazy view of the real game jar: hierarchy plus members per class. */
    private static final class GameIndex {

        private final Map<String, byte[]> bytes;
        private final Map<String, ClassNode> nodes = new HashMap<>();

        GameIndex(Path jar) throws Exception {
            bytes = readJar(jar);
            System.out.println("game jar: " + bytes.size() + " classes");
        }

        byte[] bytes(String internalName) {
            return bytes.get(internalName);
        }

        boolean exists(String internalName) {
            return bytes.containsKey(internalName);
        }

        private ClassNode node(String internalName) {
            if (!bytes.containsKey(internalName)) {
                return null;
            }
            return nodes.computeIfAbsent(internalName, key -> {
                ClassNode node = new ClassNode();
                new ClassReader(bytes.get(key)).accept(node, ClassReader.SKIP_CODE);
                return node;
            });
        }

        boolean hasMethod(String owner, String name, String descriptor) {
            Set<String> seen = new HashSet<>();
            List<String> queue = new ArrayList<>();
            queue.add(owner);
            while (!queue.isEmpty()) {
                String current = queue.remove(queue.size() - 1);
                if (!seen.add(current)) {
                    continue;
                }
                ClassNode node = node(current);
                if (node == null) {
                    continue;
                }
                for (MethodNode method : node.methods) {
                    if (method.name.equals(name) && method.desc.equals(descriptor)) {
                        return true;
                    }
                }
                if (node.superName != null) {
                    queue.add(node.superName);
                }
                queue.addAll(node.interfaces);
            }
            return false;
        }

        boolean hasField(String owner, String name, String descriptor) {
            Set<String> seen = new HashSet<>();
            List<String> queue = new ArrayList<>();
            queue.add(owner);
            while (!queue.isEmpty()) {
                String current = queue.remove(queue.size() - 1);
                if (!seen.add(current)) {
                    continue;
                }
                ClassNode node = node(current);
                if (node == null) {
                    continue;
                }
                for (FieldNode field : node.fields) {
                    if (field.name.equals(name) && field.desc.equals(descriptor)) {
                        return true;
                    }
                }
                if (node.superName != null) {
                    queue.add(node.superName);
                }
                queue.addAll(node.interfaces);
            }
            return false;
        }
    }
}
