package dev.vanta.injector;

import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Attach CLI. The jar is both the agent and the CLI, so there is nothing else to ship.
 *
 * <pre>
 *   java -jar vanta-1.0.0.jar --list
 *   java -jar vanta-1.0.0.jar --pid 4821
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path agent = agentJar();
        long pid = -1L;
        boolean list = false;
        String log = "vanta-inject.log";

        for (int i = 0; i < args.length; i++) {
            String argument = args[i];
            switch (argument) {
                case "--agent" -> agent = Path.of(next(args, ++i, "--agent"));
                case "--pid" -> pid = Long.parseLong(next(args, ++i, "--pid"));
                case "--log" -> log = next(args, ++i, "--log");
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
            List<ProcessHandle> found = candidates();
            if (found.isEmpty()) {
                System.out.println("[vanta] no running 1.21.11 client found");
            }
            for (ProcessHandle handle : found) {
                System.out.println(handle.pid() + "  " + describe(handle));
            }
            return;
        }

        if (agent == null || !agent.toFile().isFile()) {
            System.out.println("[vanta] run the CLI from the vanta jar, or pass --agent <jar>");
            return;
        }

        if (pid < 0) {
            List<ProcessHandle> found = candidates();
            if (found.isEmpty()) {
                System.out.println("[vanta] no running 1.21.11 client found; pass --pid <pid>");
                return;
            }
            pid = found.get(0).pid();
        }

        System.out.println("[vanta] attaching to " + pid + " (" + describe(ProcessHandle.of(pid).orElse(null)) + ")");
        try {
            VirtualMachine machine = VirtualMachine.attach(Long.toString(pid));
            try {
                machine.loadAgent(agent.toAbsolutePath().toString(),
                        "log=" + Path.of(log).toAbsolutePath());
            } finally {
                machine.detach();
            }
            System.out.println("[vanta] agent loaded, press RSHIFT in game");
        } catch (Throwable throwable) {
            System.out.println("[vanta] attach failed: " + throwable);
            System.out.println("[vanta] the game must run on the same JVM and a JDK, not a JRE");
        }
    }

    private static String next(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException(flag + " needs a value");
        }
        return args[index];
    }

    /** Game processes: a java command line that names 1.21.11 and the Minecraft client. */
    private static List<ProcessHandle> candidates() {
        long self = ProcessHandle.current().pid();
        List<ProcessHandle> found = new ArrayList<>();
        for (ProcessHandle handle : ProcessHandle.allProcesses().toList()) {
            if (handle.pid() == self) {
                continue;
            }
            String line = handle.info().commandLine().orElse("");
            String lower = line.toLowerCase(Locale.ROOT);
            if (!lower.contains("java") || !lower.contains("minecraft")) {
                continue;
            }
            if (lower.contains("1.21.11") || lower.contains("1_21_11")) {
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
            return Path.of(file.toURI());
        } catch (Exception exception) {
            return null;
        }
    }

    private static void usage() {
        System.out.println("""
                vanta internal client 1.21.11
                  --list              list running 1.21.11 clients
                  --pid <pid>         target process (auto-detected when omitted)
                  --agent <jar>       vanta jar to attach (defaults to this jar)
                  --log <file>        agent log path (default vanta-inject.log)
                """);
    }
}
