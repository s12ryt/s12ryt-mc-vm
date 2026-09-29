package org.bukkit;

import org.bukkit.scheduler.BukkitScheduler;

public class Server {
    private final BukkitScheduler scheduler = new BukkitScheduler();

    public BukkitScheduler getScheduler() {
        return scheduler;
    }
}
