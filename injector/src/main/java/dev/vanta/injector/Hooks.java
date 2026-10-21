package dev.vanta.injector;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Installs the loader-independent hooks.
 *
 * <p>Every insertion is straight-line code at a method entry (or immediately before a
 * return), so max stack only is recomputed and the original stack map frames stay
 * valid. Anything unexpected aborts that single class instead of corrupting it.
 */
final class Hooks {

    private static final String BRIDGE = "dev/vanta/agent/Bridge";

    private enum Kind {
        TICK,
        HUD,
        ATTACK,
        VELOCITY
    }

    private record Target(String owner, String method, String descriptor, Kind kind) {
    }

    private record Rule(String method, String descriptor, Kind kind) {
    }

    private final Instrumentation instrumentation;
    private final ClassLoader gameLoader;
    private final Resolver resolver;
    private final Map<String, List<Rule>> rules = new HashMap<>();

    private String vec3dInternal = "net/minecraft/util/math/Vec3d";

    Hooks(Instrumentation instrumentation, ClassLoader gameLoader, Resolver resolver) {
        this.instrumentation = instrumentation;
        this.gameLoader = gameLoader;
        this.resolver = resolver;
    }

    void install() {
        vec3dInternal = resolver.internal("net.minecraft.util.math.Vec3d");

        List<Class<?>> touched = new ArrayList<>();
        touched.add(register(new Target("net.minecraft.client.MinecraftClient", "tick", "()V", Kind.TICK)));
        touched.add(register(new Target("net.minecraft.client.gui.hud.InGameHud", "render",
                "(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", Kind.HUD)));
        touched.add(register(new Target("net.minecraft.entity.Entity", "setVelocityClient",
                "(Lnet/minecraft/util/math/Vec3d;)V", Kind.VELOCITY)));
        touched.add(register(new Target("net.minecraft.client.network.ClientPlayerInteractionManager", "attackEntity",
                "(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", Kind.ATTACK)));

        instrumentation.addTransformer(new Transformer(), true);
        for (Class<?> type : touched) {
            if (type == null) {
                continue;
            }
            try {
                instrumentation.retransformClasses(type);
                Log.info("hooked " + type.getName());
            } catch (Throwable throwable) {
                Log.error("retransform failed for " + type.getName(), throwable);
            }
        }
    }

    private Class<?> register(Target target) {
        try {
            String ownerInternal = resolver.internal(target.owner());
            Class<?> type = Class.forName(ownerInternal.replace('/', '.'), false, gameLoader);
            String runtimeName = resolver.methodName(target.owner(), target.method(), target.descriptor());
            int arity = Type.getArgumentTypes(target.descriptor()).length;

            Method match = null;
            for (Method candidate : type.getDeclaredMethods()) {
                if (candidate.getName().equals(runtimeName) && candidate.getParameterCount() == arity) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) {
                Log.error("hook target missing: " + target.owner() + "#" + target.method());
                return null;
            }

            String descriptor = Type.getMethodDescriptor(match);
            rules.computeIfAbsent(ownerInternal, key -> new ArrayList<>())
                    .add(new Rule(match.getName(), descriptor, target.kind()));
            Log.info("target " + ownerInternal + "#" + match.getName() + descriptor + " as " + target.kind());
            return type;
        } catch (Throwable throwable) {
            Log.error("could not register " + target.owner() + "#" + target.method(), throwable);
            return null;
        }
    }

    private final class Transformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> redefined,
                                ProtectionDomain domain, byte[] bytes) {
            if (className == null) {
                return null;
            }
            List<Rule> classRules = rules.get(className);
            if (classRules == null) {
                return null;
            }
            try {
                ClassReader reader = new ClassReader(bytes);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                     String signature, String[] exceptions) {
                        MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
                        Rule rule = null;
                        for (Rule candidate : classRules) {
                            if (candidate.method().equals(name) && candidate.descriptor().equals(descriptor)) {
                                rule = candidate;
                                break;
                            }
                        }
                        return rule == null ? delegate : new Editor(delegate, rule);
                    }
                }, 0);
                Log.info("patched " + className);
                return writer.toByteArray();
            } catch (Throwable throwable) {
                Log.error("patch failed for " + className, throwable);
                return null;
            }
        }
    }

    private final class Editor extends MethodVisitor {

        private final Rule rule;

        Editor(MethodVisitor delegate, Rule rule) {
            super(Opcodes.ASM9, delegate);
            this.rule = rule;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (rule.kind() != Kind.HUD) {
                emit(rule.kind());
            }
        }

        @Override
        public void visitInsn(int opcode) {
            // The HUD hook runs before every return so vanilla paints first.
            if (rule.kind() == Kind.HUD && opcode == Opcodes.RETURN) {
                emit(Kind.HUD);
            }
            super.visitInsn(opcode);
        }

        private void emit(Kind kind) {
            switch (kind) {
                case TICK -> super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "onTick", "()V", false);
                case HUD -> {
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "onHud", "(Ljava/lang/Object;)V", false);
                }
                case ATTACK -> {
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitVarInsn(Opcodes.ALOAD, 2);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "onAttack",
                            "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
                }
                case VELOCITY -> {
                    super.visitVarInsn(Opcodes.ALOAD, 0);
                    super.visitVarInsn(Opcodes.ALOAD, 1);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE, "filterVelocity",
                            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                    super.visitTypeInsn(Opcodes.CHECKCAST, vec3dInternal);
                    super.visitVarInsn(Opcodes.ASTORE, 1);
                }
            }
        }
    }
}
