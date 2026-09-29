package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

public class BukkitScheduler {
    public Runnable pending;

    public BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task) {
        pending = task;
        return new BukkitTask();
    }
}
