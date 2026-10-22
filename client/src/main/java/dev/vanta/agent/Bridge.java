package dev.vanta.agent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * The seam between patched vanilla bytecode and the client.
 *
 * <p>Only {@code java.base} types appear here on purpose: game classes link this on the
 * hot path and must never need a Minecraft or client class to resolve. The client fills
 * the fields once its remapping loader is up.
 */
public final class Bridge {

    public static volatile Consumer<Object> tick;
    public static volatile Consumer<Object> hud;
    public static volatile BiConsumer<Object, Object> attack;
    /** (entity, velocity) -&gt; replacement velocity, or null to keep vanilla. */
    public static volatile BiFunction<Object, Object, Object> velocity;
    /** (entity, box) -&gt; replacement box, or null to keep vanilla. */
    public static volatile BiFunction<Object, Object, Object> boundingBox;

    private Bridge() {
    }

    public static void onTick() {
        Consumer<Object> hook = tick;
        if (hook != null) {
            hook.accept(Boolean.TRUE);
        }
    }

    public static void onHud(Object context) {
        Consumer<Object> hook = hud;
        if (hook != null) {
            hook.accept(context);
        }
    }

    public static void onAttack(Object player, Object target) {
        BiConsumer<Object, Object> hook = attack;
        if (hook != null) {
            hook.accept(player, target);
        }
    }

    public static Object filterVelocity(Object entity, Object velocity) {
        BiFunction<Object, Object, Object> hook = Bridge.velocity;
        if (hook == null) {
            return velocity;
        }
        Object result = hook.apply(entity, velocity);
        return result == null ? velocity : result;
    }

    public static Object expandBox(Object entity, Object box) {
        BiFunction<Object, Object, Object> hook = boundingBox;
        if (hook == null) {
            return box;
        }
        Object result = hook.apply(entity, box);
        return result == null ? box : result;
    }
}
