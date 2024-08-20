package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.migrator.Migration;

import java.sql.Connection;

@SuppressWarnings("unused")
public class AddBuildOnColumn_2024081901 extends Migration {
    @Override
    public void up() throws Exception {
        String sql = "ALTER TABLE `websites` ADD `build_on` varchar(32) NOT NULL;";
        String setInitValuesSql = "UPDATE `websites` SET `build_on` = 'COMMIT';";

        try (Connection con = getDatabase()._getConnection()) {
            con.createStatement().execute(sql);
            con.createStatement().execute(setInitValuesSql);
        }
    }

    @Override
    public void down() throws Exception {
        String sql = "ALTER TABLE `websites` DROP `build_on`;";

        try (Connection con = getDatabase()._getConnection()) {
            con.createStatement().execute(sql);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
