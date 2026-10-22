package dev.vanta.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Patches the five call sites the client needs inside vanilla 1.21.11.
 *
 * <p>Targets are declared with Yarn names and resolved through {@link Mappings}, so no
 * obfuscated identifier is written down anywhere. Every insertion is straight-line and
 * stack-balanced at an instruction boundary, and only {@code maxStack} is raised, which
 * keeps the original stack map frames valid and makes the patch safe for
 * {@code retransformClasses} on an already running game.
 */
public final class Hooks {

    private static final String BRIDGE = "dev/vanta/agent/Bridge";
    private static final int STACK_HEADROOM = 4;
    /**
     * Guards against re-entering the transformer: a transform must never trigger
     * another load of the class it is being asked about.
     */
    private static final ThreadLocal<Boolean> BUSY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private enum Kind {
        TICK,
        HUD,
        ATTACK,
        VELOCITY,
        BOX
    }

    private record Target(String owner, String method, String descriptor, Kind kind) {
    }

    private record Rule(String method, String descriptor, Kind kind) {
    }

    private static final List<Target> TARGETS = List.of(
            new Target("net.minecraft.client.MinecraftClient", "tick", "()V", Kind.TICK),
            new Target("net.minecraft.client.gui.hud.InGameHud", "render",
                    "(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
                    Kind.HUD),
            new Target("net.minecraft.client.network.ClientPlayerInteractionManager", "attackEntity",
                    "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", Kind.ATTACK),
            new Target("net.minecraft.entity.Entity", "setVelocityClient",
                    "(Lnet/minecraft/util/math/Vec3d;)V", Kind.VELOCITY),
            new Target("net.minecraft.entity.Entity", "getBoundingBox",
                    "()Lnet/minecraft/util/math/Box;", Kind.BOX));

    private Hooks() {
    }

    /**
     * Rule set for one game version. Pure bytecode in, bytecode out, so it can be run
     * offline against the real jar.
     */
    public static final class Patcher {

        private final Map<String, List<Rule>> rules = new HashMap<>();
        private final List<String> descriptions = new ArrayList<>();

        public Patcher(Mappings mappings) {
            for (Target target : TARGETS) {
                String namedOwner = target.owner().replace('.', '/');
                String owner = mappings.officialClass(namedOwner);
                String method = mappings.method(namedOwner, target.method(), target.descriptor());
                String descriptor = mappings.officialDescriptor(target.descriptor());
                if (owner.equals(namedOwner) || method.equals(target.method())) {
                    Log.warn("no mapping for " + target.owner() + "#" + target.method() + " - hook skipped");
                    continue;
                }
                rules.computeIfAbsent(owner, key -> new ArrayList<>())
                        .add(new Rule(method, descriptor, target.kind()));
                descriptions.add(target.owner() + "#" + target.method() + " -> " + owner + "#" + method + descriptor);
            }
        }

        /**
         * Runs the whole patch pipeline on a throwaway class before the transformer is
         * installed. Without this the first transform would load ASM, collections and
         * the checker lazily inside a class definition, which is where circularity
         * errors come from.
         */
        public void warmUp() {
            for (var entry : rules.entrySet()) {
                ClassWriter writer = new ClassWriter(0);
                writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, entry.getKey(), null, "java/lang/Object", null);
                for (Rule rule : entry.getValue()) {
                    MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                            rule.method(), rule.descriptor(), null, null);
                    method.visitCode();
                    if (Type.getReturnType(rule.descriptor()).getSort() == Type.VOID) {
                        method.visitInsn(Opcodes.RETURN);
                    } else {
                        method.visitInsn(Opcodes.ACONST_NULL);
                        method.visitInsn(Opcodes.ARETURN);
                    }
                    method.visitMaxs(2, Type.getArgumentTypes(rule.descriptor()).length + 1);
                    method.visitEnd();
                }
                writer.visitEnd();
                // Same owner and same descriptors as the real target: exercises the whole
                // editor, the stack arithmetic and the bytecode checker by name.
                patch(entry.getKey(), writer.toByteArray());
            }
        }

        /** Obfuscated owners that carry at least one rule. */
        public java.util.Set<String> owners() {
            return rules.keySet();
        }

        public List<String> descriptions() {
            return descriptions;
        }

        /** How many hooks a target class should carry. */
        public int ruleCount(String owner) {
            List<Rule> classRules = rules.get(owner);
            return classRules == null ? 0 : classRules.size();
        }

        /** Patched bytes, or {@code null} when this class is not a target. */
        public byte[] patch(String className, byte[] bytes) {
            List<Rule> classRules = rules.get(className);
            if (classRules == null) {
                return null;
            }
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                 String signature, String[] exceptions) {
                    MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                    Rule rule = match(classRules, name, descriptor);
                    return rule == null ? delegate : new Editor(delegate, rule);
                }
            }, 0);
            byte[] patched = writer.toByteArray();
            // Never hand the JVM bytes we have not structurally checked.
            new ClassReader(patched).accept(new CheckClassAdapter(new ClassWriter(0), false), 0);
            return patched;
        }
    }

    /** Installs the transformer, then forces every target class through it. */
    public static void install(Instrumentation instrumentation, Mappings mappings, ClassLoader gameLoader) {
        Patcher patcher = new Patcher(mappings);
        patcher.warmUp();
        Log.info("hook targets: " + patcher.descriptions());

        // Plain hash set: keySet() on a LinkedHashMap inside a transform can request
        // LinkedHashMap$LinkedKeySet while it is being defined, which is fatal.
        Set<String> owners = new HashSet<>(patcher.owners());

        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> redefined,
                                    ProtectionDomain domain, byte[] bytes) {
                if (className == null || !owners.contains(className) || BUSY.get()) {
                    return null;
                }
                BUSY.set(Boolean.TRUE);
                try {
                    return patcher.patch(className, bytes);
                } catch (Throwable throwable) {
                    Log.error("patch failed for " + className, throwable);
                    return null;
                } finally {
                    BUSY.set(Boolean.FALSE);
                }
            }
        }, true);

        for (String owner : owners) {
            try {
                Class<?> type = Class.forName(owner.replace('/', '.'), false, gameLoader);
                instrumentation.retransformClasses(type);
                Log.info("patched " + type.getName());
            } catch (Throwable throwable) {
                Log.error("could not patch " + owner, throwable);
            }
        }
    }

    private static Rule match(List<Rule> classRules, String name, String descriptor) {
        for (Rule rule : classRules) {
            if (rule.method().equals(name) && rule.descriptor().equals(descriptor)) {
                return rule;
            }
        }
        return null;
    }

    private static final class Editor extends MethodVisitor {

        private final Rule rule;

        Editor(MethodVisitor delegate, Rule rule) {
            super(Opcodes.ASM9, delegate);
            this.rule = rule;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            switch (rule.kind()) {
                case TICK -> call("onTick", "()V");
                case ATTACK -> {
                    load(1);
                    load(2);
                    call("onAttack", "(Ljava/lang/Object;Ljava/lang/Object;)V");
                }
                case VELOCITY -> {
                    load(0);
                    load(1);
                    call("filterVelocity", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                    super.visitTypeInsn(Opcodes.CHECKCAST, Type.getArgumentTypes(rule.descriptor())[0].getInternalName());
                    super.visitVarInsn(Opcodes.ASTORE, 1);
                }
                case HUD -> {
                    // Drawn before vanilla's HUD pass. Inserting before the method's
                    // returns was rejected by HotSpot redefinition on 1.21.11's huge
                    // InGameHud#render, and a head insertion is one call site.
                    load(1);
                    call("onHud", "(Ljava/lang/Object;)V");
                }
                case BOX -> {
                    // The box is only known at the return.
                }
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (rule.kind() == Kind.BOX && opcode == Opcodes.ARETURN) {
                load(0);
                super.visitInsn(Opcodes.SWAP);
                call("expandBox", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
                super.visitTypeInsn(Opcodes.CHECKCAST, Type.getReturnType(rule.descriptor()).getInternalName());
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + STACK_HEADROOM, maxLocals);
        }

        private void load(int local) {
            super.visitVarInsn(Opcodes.ALOAD, local);
        }

        private void call(String name, String descriptor) {
            super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, name, descriptor, false);
        }
    }
}
