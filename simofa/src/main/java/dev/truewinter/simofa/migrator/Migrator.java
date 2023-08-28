package dev.truewinter.simofa.migrator;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.config.MigratorConfig;
import dev.truewinter.simofa.database.Database;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;

public class Migrator {
    private final long lastMigrationTime;
    public Migrator(Database database) {
        this.lastMigrationTime = MigratorConfig.getTimestamp();
        Migration._setDatabase(database);
    }

    public void applyMigrations() {
        if (lastMigrationTime == 0) {
            Simofa.getLogger().info("Detecting fresh installation, skipping migrations");
            try {
                MigratorConfig.migrationSuccess(MigratorConfig.createTimestamp());
            } catch (IOException e) {
                Simofa.getLogger().error("Failed to set migration time", e);
            }
            return;
        }

        try {
            List<Class<? extends Migration>> migrations = getMigrations();
            migrations.sort(Comparator.comparing(m -> Integer.parseInt(m.getSimpleName().split("_")[1])));

            for (Class<? extends Migration> migration : migrations) {
                try {
                    Simofa.getLogger().info("Running migration " + migration.getSimpleName());
                    Migration migrationInstance = migration.getDeclaredConstructor().newInstance();
                    migration.getMethod("up").invoke(migrationInstance);
                    Simofa.getLogger().info("Migration " + migration.getSimpleName() + " successful");
                } catch (Exception e) {
                    Simofa.getLogger().error("Failed to run migration " + migration.getSimpleName(), e);
                    Simofa.getLogger().info("Undoing migrations...");
                    Collections.reverse(migrations);
                    undoMigrations(migrations);
                    Simofa.shutdown();
                    System.exit(2);
                    break;
                }
            }

            if (migrations.size() > 0) {
                String migrationTimestamp = migrations.get(migrations.size() - 1).getSimpleName().split("_")[1];
                MigratorConfig.migrationSuccess(Long.parseLong(migrationTimestamp));
                Simofa.getLogger().info("Successfully migrated");
            }
        } catch (Exception e) {
            Simofa.getLogger().warn("Failed to run migrations", e);
        }
    }

    public void undoMigrations(List<Class<? extends Migration>> migrations) throws Exception {
        MigratorConfig.migrationSuccess(lastMigrationTime);

        for (Class<? extends Migration> migration : migrations) {
            Simofa.getLogger().info("Undoing migration " + migration.getSimpleName());
            Migration migrationInstance = migration.getDeclaredConstructor().newInstance();
            migration.getMethod("down").invoke(migrationInstance);
        }
    }

    private List<Class<? extends  Migration>> getMigrations() throws Exception {
        final String MIGRATION_LOCATION = "dev.truewinter.simofa.migrator.migrations";
        final String MIGRATION_LOCATION_FS = MIGRATION_LOCATION.replace(".", "/");
        List<Class<? extends Migration>> migrations = new ArrayList<>();
        URL pkg = getClass().getClassLoader().getResource(MIGRATION_LOCATION_FS);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream) Objects.requireNonNull(pkg).getContent()));

        List<String> migrationClasses = new ArrayList<>();
        if (pkg.getProtocol().equals("jar")) {
            String jarFileName = URLDecoder.decode(pkg.getFile(), StandardCharsets.UTF_8);
            jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));

            try (JarFile jarFile = new JarFile(jarFileName)) {
                jarFile.entries().asIterator().forEachRemaining(j -> {
                    if (j.getName().startsWith(MIGRATION_LOCATION_FS)) {
                        String c = j.getName()
                                .replace(MIGRATION_LOCATION_FS, "")
                                .replaceFirst("^/", "");
                        if (!c.isBlank()) {
                            migrationClasses.add(c);
                        }
                    }
                });
            }
        } else {
            String className;
            while ((className = bufferedReader.readLine()) != null) {
                migrationClasses.add(className);
            }
        }

        for (String className : migrationClasses) {
            String migrationName = className.replaceFirst("\\.class$", "");
            if (!migrationName.contains("_")) {
                throw new Exception("Migration name `" + migrationName + "` invalid, skipping");
            }
            
            // Sub-classes in the same file
            if (migrationName.contains("$")) {
                continue;
            }

            if (Long.parseLong(migrationName.split("_")[1]) <= lastMigrationTime) {
                continue;
            }

            Simofa.getLogger().info("Loading migration " + migrationName);
            try {
                URLClassLoader urlClassLoader = new URLClassLoader(
                        new URL[]{pkg},
                        getClass().getClassLoader()
                );
                Class<? extends Migration> migration = getPluginAsSubclass(urlClassLoader, MIGRATION_LOCATION + "." + migrationName);
                Migration migrationInstance = migration.getDeclaredConstructor().newInstance();
                if ((boolean) migration.getMethod("isActive").invoke(migrationInstance)) {
                    migrations.add(migration);
                } else {
                    Simofa.getLogger().info("Migration " + migrationName + " not active, skipping");
                }
            } catch (Exception e) {
                Simofa.getLogger().error("Failed to load migration class", e);
            }
        }

        return migrations;
    }

    private Class<? extends Migration> getPluginAsSubclass(URLClassLoader classLoader, String migrationClass) throws Exception {
        return Class.forName(migrationClass, false, classLoader).asSubclass(Migration.class);
    }
}
