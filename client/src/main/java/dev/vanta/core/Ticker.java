package dev.vanta.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Tick-delayed tasks so modules can sequence things like sprint resets. */
public final class Ticker {

    private record Task(int ticksLeft, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Ticker() {
    }

    public static void after(int ticks, Runnable action) {
        TASKS.add(new Task(Math.max(0, ticks), action));
    }

    public static void clear() {
        TASKS.clear();
    }

    public static void run() {
        if (TASKS.isEmpty()) {
            return;
        }
        Iterator<Task> iterator = TASKS.iterator();
        List<Runnable> due = new ArrayList<>();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.ticksLeft() <= 0) {
                due.add(task.action());
                iterator.remove();
            } else {
                TASKS.set(TASKS.indexOf(task), new Task(task.ticksLeft() - 1, task.action()));
            }
        }
        due.forEach(Runnable::run);
    }
}
