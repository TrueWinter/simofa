package dev.truewinter.simofa.deploy;

import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;

@YamlFile()
public class Config {
    @YamlKey("port")
    private int port = 8809;

    @YamlKey("key")
    private String key = "simofa";

    public int getPort() {
        return port;
    }

    public String getKey() {
        return key;
    }
}
