package org.bukkit.plugin.java;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class JavaPlugin implements Plugin {
    private final FileConfiguration config = new FileConfiguration();
    private final Server server = new Server();

    public void saveDefaultConfig() { }
    public FileConfiguration getConfig() { return config; }
    public Server getServer() { return server; }
    public File getDataFolder() { return new File("target/plugin-test"); }
    public Logger getLogger() { return Logger.getLogger("mc-vm-test"); }
    public void onEnable() { }
}
