package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.migrator.Migration;

@SuppressWarnings("unused")
public class TestMigration_2023070501 extends Migration {
    @Override
    public void up() {
        Simofa.getLogger().info(TestMigration_2023070501.class.getSimpleName() + ": Has database: " + getDatabase().getClass().getSimpleName());
        Simofa.getLogger().info(TestMigration_2023070501.class.getSimpleName() + ": Test migration successful");
    }

    @Override
    public void down() {
        Simofa.getLogger().info(TestMigration_2023070501.class.getSimpleName() + ": Undid test migration");
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
