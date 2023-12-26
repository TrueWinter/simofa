package dev.truewinter.simofaplugins.pushover;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.truewinter.simofa.api.SimofaPlugin;
import net.pushover.client.*;

@SuppressWarnings("unused")
public class PushoverPlugin extends SimofaPlugin {
    private YamlDocument config = null;
    private final PushoverClient client;

    public PushoverPlugin() {
        this.client = new PushoverRestClient();
    }

    @Override
    protected void onLoad() {
        try {
            copyDefaultConfig();
            config = YamlDocument.create(getConfig());
        } catch (Exception e) {
            getLogger().error("Failed to load config");
        }

        if (config == null) return;

        String appKey = config.getString("app_key");
        if (appKey == null || appKey.isBlank()) {
            getLogger().warn("Configuration required");
            return;
        }

        try {
            registerListeners(this, new EventListeners(this, config, getLogger()));
        } catch (Exception e) {
            getLogger().error("Failed to register listeners", e);
        }
    }

    @Override
    protected void onUnload() {

    }

    public void pushMessage(String title, String msg) {
        try {
            client.pushMessage(PushoverMessage.builderWithApiToken(config.getString("app_key"))
                    .setUserId(config.getString("user_key"))
                    .setTitle(title)
                    .setMessage(msg)
                    .build());
        } catch (PushoverException e) {
            getLogger().error("Failed to send PushOver message", e);
        }
    }
}
