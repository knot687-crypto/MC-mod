package dev.vanta.agent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The only class shared between the injected bytecode and the client.
 *
 * <p>It deliberately references nothing but java.base types: the game class loader
 * links this on the hot path, and it must never need a Minecraft or client class to
 * resolve. The client wires the four function fields once its own loader is up.
 */
public final class Bridge {

    public static volatile Consumer<Object> tick;
    public static volatile Consumer<Object> hud;
    public static volatile Function<Object, Object> velocityFilter;
    public static volatile BiConsumer<Object, Object> attack;

    private Bridge() {
    }

    public static void onTick() {
        Consumer<Object> hook = tick;
        if (hook != null) {
            hook.accept(null);
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

    /** Returns the velocity to apply; the original object when nothing wants to change it. */
    public static Object filterVelocity(Object self, Object velocity) {
        Function<Object, Object> hook = velocityFilter;
        if (hook == null) {
            return velocity;
        }
        Object result = hook.apply(new Object[]{self, velocity});
        return result == null ? velocity : result;
    }
}
