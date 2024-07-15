package dev.truewinter.simofa.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.truewinter.simofa.Simofa;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class MigratorConfig {
    private static YamlDocument config;
    private static long timestamp;

    public static void init(File file) throws IOException {
        config = YamlDocument.create(
                file,
                Objects.requireNonNull(Simofa.class.getClassLoader().getResourceAsStream("migrator.yml")),
                GeneralSettings.builder(GeneralSettings.DEFAULT).setUseDefaults(false).build(),
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning("config-version")).build()
        );

        timestamp = config.getLong("migration_time");
    }

    public static long createTimestamp() {
        Calendar calendar = Calendar.getInstance();
        return Long.parseLong(String.valueOf(calendar.get(Calendar.YEAR)) +
            convertToPaddedString(calendar.get(Calendar.MONTH) + 1, 2) +
            convertToPaddedString(calendar.get(Calendar.DAY_OF_MONTH), 2) +
            "01");
    }

    @SuppressWarnings("SameParameterValue")
    private static String convertToPaddedString(int num, int length) {
        StringBuilder out = new StringBuilder();
        String iString = String.valueOf(num);
        if (iString.length() < length) {
            out.append("0".repeat(Math.max(0, length - iString.length())));
        }

        out.append(iString);
        return out.toString();
    }

    public static long getTimestamp() {
        return timestamp;
    }

    public static void migrationSuccess(long ts) throws IOException {
        timestamp = ts;
        config.set("migration_time", timestamp);
        config.save();
    }
}
