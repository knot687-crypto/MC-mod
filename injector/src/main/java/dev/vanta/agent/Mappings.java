package dev.vanta.agent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Named (Yarn) to runtime (obfuscated) mapping table, generated offline by
 * {@code tools/MappingGen.java} from the 1.21.11 Yarn merged mappings and the real
 * client jar.
 *
 * <p>Member lookups walk the class hierarchy, because javac records the static type of
 * the receiver: {@code mc.player.getPos()} is emitted against
 * {@code ClientPlayerEntity} even though {@code getPos} lives on {@code Entity}.
 */
public final class Mappings {

    /** Separates owner, name and descriptor inside a member key. */
    private static final char KEY = '\u0000';

    private final Map<String, String> classes = new HashMap<>(16384);
    private final Map<String, String> methods = new HashMap<>(131072);
    private final Map<String, String> fields = new HashMap<>(32768);
    private final Map<String, String[]> hierarchy = new HashMap<>(16384);
    /** Client classes, registered before they are remapped. */
    private final Map<String, String[]> own = new ConcurrentHashMap<>();

    private Mappings() {
    }

    public static Mappings load() {
        Mappings mappings = new Mappings();
        try (InputStream raw = Mappings.class.getResourceAsStream("/vanta/mappings.bin")) {
            if (raw == null) {
                throw new IllegalStateException("vanta/mappings.bin missing from the jar");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.charAt(0) == '#') {
                        continue;
                    }
                    String[] parts = line.split("\t", -1);
                    switch (parts[0]) {
                        case "C" -> mappings.classes.put(parts[1], parts[2]);
                        case "M" -> mappings.methods.put(key(parts[1], parts[2], parts[3]), parts[4]);
                        case "F" -> mappings.fields.put(key(parts[1], parts[2], parts[3]), parts[4]);
                        case "H" -> {
                            // H <name> <super> <interfaces, comma separated>
                            String value = parts.length > 3 && !parts[3].isEmpty()
                                    ? parts[2] + "," + parts[3]
                                    : parts[2];
                            mappings.hierarchy.put(parts[1],
                                    value.isEmpty() ? new String[0] : value.split(","));
                        }
                        default -> {
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("could not read vanta mappings", exception);
        }
        return mappings;
    }

    private static String key(String owner, String name, String descriptor) {
        return owner + KEY + name + KEY + descriptor;
    }

    /** Named internal name to the name the game actually runs. */
    public String officialClass(String named) {
        return classes.getOrDefault(named, named);
    }

    /** Registers a client class so its own overrides resolve through the game hierarchy. */
    public void addOwn(String internalName, String superName, String[] interfaces) {
        String[] entries = new String[interfaces.length];
        System.arraycopy(interfaces, 0, entries, 0, interfaces.length);
        own.put(internalName, entries);
        hierarchy.put(internalName, entries.length == 0
                ? new String[]{superName}
                : prepend(superName, entries));
    }

    private static String[] prepend(String first, String[] rest) {
        String[] out = new String[rest.length + 1];
        out[0] = first;
        System.arraycopy(rest, 0, out, 1, rest.length);
        return out;
    }

    /** Rewrites every class token in a descriptor from named to official. */
    public String officialDescriptor(String named) {
        if (named == null || named.isEmpty()) {
            return named;
        }
        StringBuilder out = new StringBuilder(named.length() + 8);
        int i = 0;
        while (i < named.length()) {
            char c = named.charAt(i);
            if (c == 'L') {
                int end = named.indexOf(';', i);
                if (end < 0) {
                    return named;
                }
                out.append('L').append(officialClass(named.substring(i + 1, end))).append(';');
                i = end + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * True when a super type of {@code owner} maps this member, i.e. the member is an
     * override and its emitted name must differ from the source name.
     */
    public boolean inherited(String owner, boolean method, String name, String descriptor) {
        String[] parents = hierarchy.get(owner);
        if (parents == null) {
            return false;
        }
        Map<String, String> table = method ? methods : fields;
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        queue.add(owner);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!seen.add(current)) {
                continue;
            }
            String[] next = hierarchy.get(current);
            if (next == null) {
                continue;
            }
            for (String parent : next) {
                if (parent == null || parent.isEmpty()) {
                    continue;
                }
                if (table.containsKey(key(parent, name, descriptor))) {
                    return true;
                }
                queue.add(parent);
            }
        }
        return false;
    }

    /** Runtime name of a method, or the given name when Yarn never mapped it. */
    public String method(String owner, String name, String namedDescriptor) {
        return resolve(methods, owner, name, namedDescriptor);
    }

    /** Runtime name of a field, or the given name when Yarn never mapped it. */
    public String field(String owner, String name, String namedDescriptor) {
        return resolve(fields, owner, name, namedDescriptor);
    }

    private String resolve(Map<String, String> table, String owner, String name, String descriptor) {
        String mapped = table.get(key(owner, name, descriptor));
        if (mapped != null) {
            return mapped;
        }
        if (!hierarchy.containsKey(owner)) {
            return name;
        }
        // Walk the whole hierarchy: the bytecode owner is the static type of the
        // receiver, not the class that declares the member.
        java.util.ArrayDeque<String> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        queue.add(owner);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (!seen.add(current)) {
                continue;
            }
            String[] parents = hierarchy.get(current);
            if (parents == null) {
                continue;
            }
            for (String parent : parents) {
                if (parent == null || parent.isEmpty()) {
                    continue;
                }
                mapped = table.get(key(parent, name, descriptor));
                if (mapped != null) {
                    return mapped;
                }
                queue.add(parent);
            }
        }
        return name;
    }

    public int classCount() {
        return classes.size();
    }

    public int memberCount() {
        return methods.size() + fields.size();
    }
}
