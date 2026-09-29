package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

public interface BukkitScheduler {
    BukkitTask runTaskAsynchronously(Plugin plugin, Runnable task);
}
