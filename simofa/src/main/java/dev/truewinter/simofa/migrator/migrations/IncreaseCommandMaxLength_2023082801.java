package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.migrator.Migration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class IncreaseCommandMaxLength_2023082801 extends Migration {
    private static final String COMMAND_TEMPLATE = "ALTER TABLE %s MODIFY COLUMN %s VARCHAR(%d);";

    @Override
    public void up() throws Exception {
        List<MigrationCommand> commands = new ArrayList<>();
        commands.add(new MigrationCommand("templates", "template", 4000));
        commands.add(new MigrationCommand("websites", "build_command", 512));
        commands.add(new MigrationCommand("websites", "deployment_command", 512));
        commands.add(new MigrationCommand("websites", "deployment_failed_command", 512));

        runCommands(commands);
    }

    @Override
    public void down() throws Exception {
        List<MigrationCommand> commands = new ArrayList<>();
        commands.add(new MigrationCommand("templates", "template", 2000));
        commands.add(new MigrationCommand("websites", "build_command", 255));
        commands.add(new MigrationCommand("websites", "deployment_command", 255));
        commands.add(new MigrationCommand("websites", "deployment_failed_command", 255));

        runCommands(commands);
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private void runCommands(List<MigrationCommand> commands) throws SQLException {
        try (Connection connection = getDatabase()._getConnection()) {
            for (MigrationCommand migrationCommand : commands) {
                connection.createStatement().execute(migrationCommand.command());
            }
        }
    }

    private record MigrationCommand(String table, String column, int length) {
        private String command() {
            return String.format(COMMAND_TEMPLATE, table, column, length);
        }
    }
}
