package tw.cute.mcvm;

import java.nio.file.Path;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public final class McVmPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        final VmSettings settings;
        try {
            settings = new VmSettings(getConfig().getString("ssh-public-key", ""),
                    getConfig().getInt("memory-mb", 1024), getConfig().getInt("cpus", 1),
                    getConfig().getInt("ssh-port", 2222));
        } catch (IllegalArgumentException invalid) {
            getLogger().severe("VM not started: configure a valid ssh-public-key in config.yml: "
                    + invalid.getMessage());
            return;
        }

        final Path directory = getDataFolder().toPath().resolve("vm");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            try {
                getLogger().info("Preparing Debian VM in " + directory + " (first download may take time)");
                new VmProvisioner(directory, settings, new DebianImageSource(),
                        new SystemCommands(directory), new SystemVmState()).provision();
                getLogger().info("Debian VM started or already running; SSH via configured port");
            } catch (Exception failure) {
                getLogger().log(Level.SEVERE, "Debian VM installation/start failed; data preserved", failure);
            }
        });
    }
}
