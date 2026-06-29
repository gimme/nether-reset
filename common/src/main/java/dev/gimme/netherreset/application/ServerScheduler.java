package dev.gimme.netherreset.application;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Runs a {@link Runnable} a fixed number of server ticks later; the platform layer pumps {@link #tick()} once
 * per server tick. Exists because Minecraft has no "run in N ticks" primitive — {@code MinecraftServer.schedule}
 * drains its queue as soon as the server has spare time, ignoring the target tick. Server-thread only, so no
 * synchronization.
 */
public final class ServerScheduler {

    private static final class Task {
        private int remaining;
        private final Runnable action;

        private Task(int remaining, Runnable action) {
            this.remaining = remaining;
            this.action = action;
        }
    }

    private final List<Task> tasks = new ArrayList<>();

    /** Runs {@code action} after {@code delayTicks} server ticks (at least one). */
    public void schedule(int delayTicks, Runnable action) {
        tasks.add(new Task(Math.max(1, delayTicks), action));
    }

    /** Advances pending tasks and runs those that come due. */
    public void tick() {
        if (tasks.isEmpty()) return;

        // Run due actions only after iterating, so one that schedules more work can't mutate the list mid-loop.
        List<Runnable> due = null;
        for (Iterator<Task> it = tasks.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (--task.remaining <= 0) {
                it.remove();
                if (due == null) due = new ArrayList<>();
                due.add(task.action);
            }
        }
        if (due != null) due.forEach(Runnable::run);
    }
}
