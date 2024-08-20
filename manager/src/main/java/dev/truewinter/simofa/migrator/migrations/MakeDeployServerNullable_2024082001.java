package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.migrator.Migration;

import java.sql.Connection;

@SuppressWarnings("unused")
public class MakeDeployServerNullable_2024082001 extends Migration {
    @Override
    public void up() throws Exception {
        String sql = "ALTER TABLE `websites` MODIFY COLUMN `deployment_server` CHAR(36);";

        try (Connection con = getDatabase()._getConnection()) {
            con.createStatement().execute(sql);
        }
    }

    @Override
    public void down() throws Exception {
        String sql = "ALTER TABLE `websites` MODIFY COLUMN `deployment_server` CHAR(36) NOT NULL;";

        try (Connection con = getDatabase()._getConnection()) {
            con.createStatement().execute(sql);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
