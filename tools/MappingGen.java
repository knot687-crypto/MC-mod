import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Turns the Yarn merged mappings (official / intermediary / named) plus the real
 * obfuscated 1.21.11 jar into the compact table the agent parses at runtime.
 *
 * <pre>
 *   java tools/MappingGen.java mappings.tiny client.jar vanta/mappings.bin
 * </pre>
 *
 * Output is line based and gzipped. Descriptors are rewritten into the named
 * namespace because that is the namespace the client sources compile against.
 *
 * <pre>
 *   C named official                     class rename
 *   H named super iface,iface            named hierarchy
 *   M owner name desc officialName       method rename
 *   F owner name desc officialName       field rename
 * </pre>
 */
public final class MappingGen {

    private static final Map<String, String> officialToNamed = new HashMap<>(8192);
    private static final Map<String, String> namedToOfficial = new HashMap<>(8192);

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("usage: MappingGen <mappings.tiny> <obf-client.jar> <out>");
            System.exit(2);
        }
        var classes = new TreeMap<String, String>();
        var members = new TreeSet<String>();
        var hierarchy = new TreeMap<String, String>();

        parseTiny(args[0], classes, members);
        readHierarchy(args[1], hierarchy);

        try (Writer out = new BufferedWriter(new OutputStreamWriter(
                new GZIPOutputStream(new FileOutputStream(args[2]), 1 << 16), StandardCharsets.UTF_8))) {
            out.write("# vanta runtime mappings 1.21.11\n");
            for (var entry : classes.entrySet()) {
                out.write("C\t" + entry.getKey() + "\t" + entry.getValue() + "\n");
            }
            for (String line : members) {
                out.write(line);
                out.write('\n');
            }
            for (var entry : hierarchy.entrySet()) {
                out.write("H\t" + entry.getKey() + "\t" + entry.getValue() + "\n");
            }
        }
        System.out.println("classes=" + classes.size() + " members=" + members.size()
                + " hierarchy=" + hierarchy.size());
    }

    /** tiny v2 with the namespaces official, intermediary, named. Two passes: the
     *  class map has to be complete before member descriptors can be rewritten. */
    private static void parseTiny(String path, Map<String, String> classes, TreeSet<String> members) throws IOException {
        java.util.List<String> lines = java.nio.file.Files.readAllLines(java.nio.file.Path.of(path), StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).startsWith("tiny\t2")) {
            throw new IllegalStateException("not a tiny v2 file: " + path);
        }
        String[] namespaces = lines.get(0).split("\t");
        int officialIndex = -1;
        int namedIndex = -1;
        for (int i = 3; i < namespaces.length; i++) {
            if ("official".equals(namespaces[i])) {
                officialIndex = i - 3;
            }
            if ("named".equals(namespaces[i])) {
                namedIndex = i - 3;
            }
        }
        if (officialIndex < 0 || namedIndex < 0) {
            throw new IllegalStateException("official/named namespace missing: " + lines.get(0));
        }

        for (String line : lines) {
            if (!line.startsWith("c\t")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            String official = parts[1 + officialIndex];
            String named = parts[1 + namedIndex];
            if (official.isEmpty() || named.isEmpty()) {
                continue;
            }
            officialToNamed.put(official, named);
            namedToOfficial.put(named, official);
            classes.put(named, official);
        }

        String ownerNamed = null;
        for (String line : lines) {
            if (line.startsWith("c\t")) {
                String[] parts = line.split("\t", -1);
                ownerNamed = parts[1 + namedIndex];
                continue;
            }
            if (ownerNamed == null || ownerNamed.isEmpty() || !line.startsWith("\t")) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            if (parts.length < 3 + namedIndex) {
                continue;
            }
            String kind = parts[1];
            if (!"m".equals(kind) && !"f".equals(kind)) {
                continue;
            }
            String officialDescriptor = parts[2];
            String[] names = new String[namedIndex + 1];
            System.arraycopy(parts, 3, names, 0, Math.min(names.length, parts.length - 3));
            String officialName = names[officialIndex];
            String namedName = names[namedIndex];
            if (officialName == null || officialName.isEmpty()
                    || namedName == null || namedName.isEmpty()
                    || "<init>".equals(namedName) || "<clinit>".equals(namedName)) {
                continue;
            }
            String namedDescriptor = descriptor(officialDescriptor);
            if (namedDescriptor == null) {
                continue;
            }
            members.add(("m".equals(kind) ? "M" : "F") + "\t" + ownerNamed + "\t" + namedName
                    + "\t" + namedDescriptor + "\t" + officialName);
        }
    }

    /** Rewrites every class token in a descriptor from official to named. */
    private static String descriptor(String official) {
        StringBuilder out = new StringBuilder(official.length() + 8);
        int i = 0;
        while (i < official.length()) {
            char c = official.charAt(i);
            if (c == 'L') {
                int end = official.indexOf(';', i);
                if (end < 0) {
                    return null;
                }
                String type = official.substring(i + 1, end);
                String named = officialToNamed.get(type);
                if (named == null) {
                    if (type.contains("$")) {
                        // Nested classes are mapped through their outer name when Yarn kept the suffix.
                        int dollar = type.indexOf('$');
                        String outer = officialToNamed.get(type.substring(0, dollar));
                        named = outer == null ? type : outer + type.substring(dollar);
                    } else {
                        named = type;
                    }
                }
                out.append('L').append(named).append(';');
                i = end + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Reads every class header so the runtime remapper can walk super types. */
    private static void readHierarchy(String jarPath, Map<String, String> hierarchy) throws IOException {
        var counts = new HashMap<String, Integer>();
        try (var zip = new ZipInputStream(new FileInputStream(jarPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (!name.endsWith(".class") || name.startsWith("META-INF/")) {
                    continue;
                }
                byte[] bytes = zip.readAllBytes();
                String[] header = new ClassHeader(bytes).read();
                if (header == null) {
                    continue;
                }
                String self = header[0];
                String named = officialToNamed.get(self);
                if (named == null) {
                    continue;
                }
                counts.merge(named, 1, Integer::sum);
                String superName = header[1];
                String namedSuper = superName == null ? "" : officialToNamed.getOrDefault(superName, superName);
                List<String> interfaces = new ArrayList<>();
                for (String iface : header[2].isEmpty() ? new String[0] : header[2].split(",")) {
                    interfaces.add(officialToNamed.getOrDefault(iface, iface));
                }
                hierarchy.put(named, namedSuper + "\t" + String.join(",", interfaces));
            }
        }
        // Duplicate named classes (remapped synthetics) are dropped: the first header wins.
        hierarchy.entrySet().removeIf(e -> counts.getOrDefault(e.getKey(), 0) > 1);
    }

    /** Minimal class file reader: this name, super name, interface list. */
    private static final class ClassHeader {

        private final byte[] bytes;

        ClassHeader(byte[] bytes) {
            this.bytes = bytes;
        }

        String[] read() {
            try (var in = new DataInputStream(new java.io.ByteArrayInputStream(bytes))) {
                if (in.readInt() != 0xCAFEBABE) {
                    return null;
                }
                in.readUnsignedShort();
                in.readUnsignedShort();
                int count = in.readUnsignedShort();
                String[] utf8 = new String[count];
                int[] classNameIndex = new int[count];
                for (int i = 1; i < count; i++) {
                    int tag = in.readUnsignedByte();
                    switch (tag) {
                        case 1 -> utf8[i] = in.readUTF();
                        case 3, 4 -> in.skipNBytes(4);
                        case 5, 6 -> {
                            in.skipNBytes(8);
                            i++;
                        }
                        case 7 -> classNameIndex[i] = in.readUnsignedShort();
                        case 8, 16, 19, 20 -> in.skipNBytes(2);
                        case 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
                        case 15 -> in.skipNBytes(3);
                        default -> {
                            return null;
                        }
                    }
                }
                in.readUnsignedShort();
                String self = resolve(utf8, classNameIndex, in.readUnsignedShort());
                int superIndex = in.readUnsignedShort();
                String superName = superIndex == 0 ? null : resolve(utf8, classNameIndex, superIndex);
                int interfaceCount = in.readUnsignedShort();
                StringBuilder interfaces = new StringBuilder();
                for (int i = 0; i < interfaceCount; i++) {
                    if (i > 0) {
                        interfaces.append(',');
                    }
                    interfaces.append(resolve(utf8, classNameIndex, in.readUnsignedShort()));
                }
                return new String[]{self, superName, interfaces.toString()};
            } catch (IOException exception) {
                return null;
            }
        }

        private static String resolve(String[] utf8, int[] classNameIndex, int index) {
            if (index <= 0 || index >= classNameIndex.length) {
                return "";
            }
            int utf8Index = classNameIndex[index];
            return utf8Index > 0 && utf8Index < utf8.length && utf8[utf8Index] != null ? utf8[utf8Index] : "";
        }
    }
}
