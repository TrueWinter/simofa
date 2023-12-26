# Pushover Plugin

This plugin sends a message to your device through Pushover if the build enters a configured state (by default, `stopped` or `error`).

## Installation

Place the JAR file in the `plugins` directory.

```
Simofa/
    config.yml
    Simofa-0.0.0.jar
    plugins/
        pushover-plugin-0.0.0.jar
```

## Configuration

The first time you run Simofa after installing this plugin, the default configuration will be copied to `plugins/Pushover/config.yml`. Configure as necessary and then restart Simofa.