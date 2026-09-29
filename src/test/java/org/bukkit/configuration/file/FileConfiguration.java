package org.bukkit.configuration.file;

public class FileConfiguration {
    public String key = "";

    public String getString(String name, String fallback) {
        return key;
    }

    public int getInt(String name, int fallback) {
        return fallback;
    }
}
