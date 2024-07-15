package dev.truewinter.simofa.migrator;

import dev.truewinter.simofa.database.Database;

@SuppressWarnings("unused")
public abstract class Migration {
    private static Database database;

    public abstract void up() throws Exception;
    public abstract void down() throws Exception;
    public abstract boolean isActive();

    protected static void _setDatabase(Database database) {
        Migration.database = database;
    }

    protected Database getDatabase() {
        return database;
    }
}
