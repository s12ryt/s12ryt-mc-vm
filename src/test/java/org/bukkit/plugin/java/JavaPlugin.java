package org.bukkit.plugin.java;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class JavaPlugin implements Plugin {
    private final FileConfiguration config = new FileConfiguration();
    private Runnable pending;
    private final Server server = () -> (plugin, task) -> {
        pending = task;
        return new org.bukkit.scheduler.BukkitTask() { };
    };

    public void saveDefaultConfig() { }
    public FileConfiguration getConfig() { return config; }
    public Server getServer() { return server; }
    public Runnable pendingTask() { return pending; }
    public File getDataFolder() { return new File("target/plugin-test"); }
    public Logger getLogger() { return Logger.getLogger("mc-vm-test"); }
    public void onEnable() { }
}
