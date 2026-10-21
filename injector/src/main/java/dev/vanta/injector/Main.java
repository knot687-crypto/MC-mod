package dev.vanta.injector;

import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Attach CLI.
 *
 * <pre>
 *   java -jar vanta-injector.jar --list
 *   java -jar vanta-injector.jar --pid 4821 --client vanta-client-1.0.0.jar
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path agent = agentJar();
        Path client = null;
        long pid = -1L;
        boolean list = false;

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            switch (argument) {
                case "--agent" -> agent = Path.of(next(args, ++i, "--agent"));
                case "--client" -> client = Path.of(next(args, ++i, "--client"));
                case "--pid" -> pid = Long.parseLong(next(args, ++i, "--pid"));
                case "--list" -> list = true;
                case "--help", "-h" -> {
                    usage();
                    return;
                }
                default -> {
                    if (argument.matches("\\d+")) {
                        pid = Long.parseLong(argument);
                    }
                }
            }
        }

        if (list) {
            for (ProcessHandle handle : candidates()) {
                System.out.println(handle.pid() + "  " + describe(handle));
            }
            return;
        }

        if (agent == null || !agent.toFile().isFile()) {
            System.out.println("[vanta] injector jar not found; pass --agent <path>");
            return;
        }
        if (client == null) {
            Path beside = agent.toAbsolutePath().getParent();
            try (var stream = Files.list(beside)) {
                client = stream.filter(path -> path.getFileName().toString().startsWith("vanta-client")
                                && path.getFileName().toString().endsWith(".jar"))
                        .findFirst().orElse(null);
            } catch (Exception ignored) {
                client = null;
            }
        }
        if (client == null || !client.toFile().isFile()) {
            System.out.println("[vanta] client jar not found; pass --client <path>");
            return;
        }

        if (pid < 0) {
            List<ProcessHandle> found = candidates();
            if (found.isEmpty()) {
                System.out.println("[vanta] no running 1.21.11 instance found; start the game first or pass --pid");
                return;
            }
            pid = found.get(0).pid();
        }

        String target = Long.toString(pid);
        System.out.println("[vanta] attaching to " + pid + " (" + describe(ProcessHandle.of(pid).orElse(null)) + ")");
        try {
            VirtualMachine machine = VirtualMachine.attach(target);
            try {
                String options = "client=" + client.toAbsolutePath() + ";log="
                        + Path.of("vanta-inject.log").toAbsolutePath();
                machine.loadAgent(agent.toAbsolutePath().toString(), options);
            } finally {
                machine.detach();
            }
            System.out.println("[vanta] agent loaded - press RSHIFT in game");
        } catch (Throwable throwable) {
            System.out.println("[vanta] attach failed: " + throwable);
            System.out.println("[vanta] the game must run on the same account and a JDK, not a JRE");
        }
    }

    private static String next(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " needs a value");
        }
        return args[index];
    }

    private static List<ProcessHandle> candidates() {
        long self = ProcessHandle.current().pid();
        List<ProcessHandle> found = new ArrayList<>();
        for (ProcessHandle handle : ProcessHandle.allProcesses().toList()) {
            if (handle.pid() == self) {
                continue;
            }
            String line = handle.info().commandLine().orElse("");
            if (!line.contains("java")) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            boolean fabric = lower.contains("knotclient") || lower.contains("fabric");
            boolean version = lower.contains("1.21.11") || lower.contains("1_21_11");
            if (fabric && version) {
                found.add(handle);
            }
        }
        return found;
    }

    private static String describe(ProcessHandle handle) {
        if (handle == null) {
            return "unknown";
        }
        return handle.info().command().orElse("java");
    }

    private static Path agentJar() {
        try {
            File file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return file.toPath();
        } catch (Exception exception) {
            return null;
        }
    }

    private static void usage() {
        System.out.println("""
                vanta injector
                  --list                 list running 1.21.11 instances
                  --pid <pid>            target process (auto-detected when omitted)
                  --agent <jar>          injector jar (defaults to this jar)
                  --client <jar>         client jar (defaults to a sibling vanta-client-*.jar)
                """);
    }
}
