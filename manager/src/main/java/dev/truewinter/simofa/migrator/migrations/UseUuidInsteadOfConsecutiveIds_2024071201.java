package dev.truewinter.simofa.migrator.migrations;

import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.migrator.Migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
public class UseUuidInsteadOfConsecutiveIds_2024071201 extends Migration {
    private static final String ALTER_COMMAND_TEMPLATE = "ALTER TABLE %s MODIFY COLUMN %s CHAR(36);";
    private static final String UPDATE_COMMAND_TEMPLATE = "UPDATE %s SET %s = %s WHERE id = '%s';";

    @Override
    public void up() throws Exception {
        // table: old ID, new UUID
        HashMap<String, HashMap<Integer, String>> idMap = new HashMap<>();
        
        List<String> tables = new ArrayList<>(){{
            add("websites");
            add("deployment_servers");
            add("git");
            add("users");
            add("templates");
        }};

        try (Connection connection = getDatabase()._getConnection()) {
            List<String> sql = new ArrayList<>();
            sql.add("SET FOREIGN_KEY_CHECKS = 0;");

            for (String table : tables) {
                sql.add(String.format(ALTER_COMMAND_TEMPLATE, table, "id"));
                if (table.equals("websites")) {
                    sql.add(String.format(ALTER_COMMAND_TEMPLATE, table, "git_credential"));
                    sql.add(String.format(ALTER_COMMAND_TEMPLATE, table, "deployment_server"));
                }

                HashMap<Integer, String> tableIdMap = getIds(connection, table);
                idMap.put(table, tableIdMap);
                tableIdMap.forEach((oldId, newId) -> {
                    sql.add(String.format(UPDATE_COMMAND_TEMPLATE, table, "id", quote(newId), oldId));
                });
            }

            HashMap<String, String> gitReferences = getWebsiteReferencedIds(connection, "git_credential",
                    "git", idMap);
            HashMap<String, String> deploymentServerReferences = getWebsiteReferencedIds(connection,
                    "deployment_server", "deployment_servers", idMap);

            gitReferences.forEach((websiteId, gitId) -> {
                sql.add(String.format(UPDATE_COMMAND_TEMPLATE, "websites", "git_credential",
                        gitId == null ? null : quote(gitId), websiteId));
            });

            deploymentServerReferences.forEach((websiteId, deplId) -> {
                sql.add(String.format(UPDATE_COMMAND_TEMPLATE, "websites", "deployment_server", quote(deplId),
                        websiteId));
            });

            sql.add("SET FOREIGN_KEY_CHECKS = 1;");

            for (String s : sql) {
                connection.createStatement().execute(s);
            }
        }
    }

    @Override
    public void down() throws Exception {
        throw new Exception("This migration cannot be rolled back");
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private HashMap<Integer, String> getIds(Connection connection, String table) throws SQLException {
        HashMap<Integer, String> idMap = new HashMap<>();
        String command = String.format("SELECT id FROM %s", table);

        ResultSet rs = connection.createStatement().executeQuery(command);
        while (rs.next()) {
            idMap.put(rs.getInt("id"), Util.createv7UUID().toString());
        }

        return idMap;
    }

    private HashMap<String, String> getWebsiteReferencedIds(Connection connection, String column, String references,
                                                            HashMap<String, HashMap<Integer, String>> idMap) throws SQLException {
        // website id, column id
        HashMap<String, String> referencedIds = new HashMap<>();
        String command = String.format("SELECT id, %s from websites", column);

        ResultSet rs = connection.createStatement().executeQuery(command);
        while (rs.next()) {
            String websiteId = idMap.get("websites").get(rs.getInt("id"));
            String columnId = idMap.get(references).get(rs.getInt(column));
            referencedIds.put(websiteId, columnId);
        }

        return referencedIds;
    }

    private String quote(String str) {
        return "'" + str + "'";
    }
}
