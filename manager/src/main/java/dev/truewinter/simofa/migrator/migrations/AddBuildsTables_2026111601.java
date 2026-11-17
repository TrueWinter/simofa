package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.migrator.Migration;

import java.sql.Connection;

@SuppressWarnings("unused")
public class AddBuildsTables_2026111601 extends Migration {
    @Override
    public void up() throws Exception {
        String buildsSql = "CREATE TABLE IF NOT EXISTS `builds` (" +
                "`id` char(36) NOT NULL PRIMARY KEY, " +
                "`website_id` char(36) NOT NULL, " +
                "`commit_message` text NOT NULL, " +
                "`status` varchar(16) NOT NULL, " +
                "`queued_at` bigint NOT NULL, " +
                "`start_time` bigint NOT NULL DEFAULT 0, " +
                "`end_time` bigint NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (`website_id`) REFERENCES `websites` (`id`) ON DELETE CASCADE, " +
                "INDEX `idx_builds_website_id` (`website_id`, `id`) " +
                ") CHARACTER SET utf8 COLLATE utf8_unicode_ci;";

        String logsSql = "CREATE TABLE IF NOT EXISTS `build_logs` (" +
                "`id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, " +
                "`build_id` char(36) NOT NULL, " +
                "`type` varchar(10) NOT NULL, " +
                "`message` mediumtext NOT NULL, " +
                "`uuid` char(36) NOT NULL, " +
                "`timestamp` bigint NOT NULL, " +
                "FOREIGN KEY (`build_id`) REFERENCES `builds` (`id`) ON DELETE CASCADE, " +
                "INDEX `idx_build_logs_build_time` (`build_id`, `timestamp`) " +
                ") CHARACTER SET utf8 COLLATE utf8_unicode_ci;";

        try (Connection con = getDatabase()._getConnection()) {
            con.createStatement().execute(buildsSql);
            con.createStatement().execute(logsSql);
        }
    }

    @Override
    public void down() throws Exception {
        try (Connection con = getDatabase()._getConnection()) {
            con.createStatement().execute("DROP TABLE IF EXISTS `build_logs`;");
            con.createStatement().execute("DROP TABLE IF EXISTS `builds`;");
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
