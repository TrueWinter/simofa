package dev.truewinter.simofa.migrator.migrations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.truewinter.simofa.migrator.Migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
public class MigrateTemplatesDeployProperties_2024073001 extends Migration {
    private final HashMap<String, String> PROPERTIES_TO_RENAME = new HashMap<>(){{
        put("deploymentCommand", "deployCommand");
        put("deploymentFailedCommand", "deployFailedCommand");
    }};

    @Override
    public void up() throws Exception {
        try (Connection connection = getDatabase()._getConnection()) {
            HashMap<String, ObjectNode> templates = getTemplates(connection);

            for (Map.Entry<String, ObjectNode> entry : templates.entrySet()) {
                String id = entry.getKey();
                ObjectNode template = entry.getValue();

                PROPERTIES_TO_RENAME.forEach((oldProp, newProp) -> {
                    if (template.has(oldProp)) {
                        template.put(newProp, template.get(oldProp).asText());
                        template.remove(oldProp);
                    }
                });

                PreparedStatement stmt = connection.prepareStatement("UPDATE templates SET template = ? WHERE id = ?;");
                stmt.setString(1, template.toString());
                stmt.setString(2, id);
                stmt.execute();
            }
        }
    }

    @Override
    public void down() throws Exception {
        try (Connection connection = getDatabase()._getConnection()) {
            HashMap<String, ObjectNode> templates = getTemplates(connection);

            for (Map.Entry<String, ObjectNode> entry : templates.entrySet()) {
                String id = entry.getKey();
                ObjectNode template = entry.getValue();

                PROPERTIES_TO_RENAME.forEach((oldProp, newProp) -> {
                    if (template.has(newProp)) {
                        template.put(oldProp, template.get(newProp).asText());
                        template.remove(newProp);
                    }
                });

                PreparedStatement stmt = connection.prepareStatement("UPDATE templates SET template = ? WHERE id = ?;");
                stmt.setString(1, template.toString());
                stmt.setString(2, id);
                stmt.execute();
            }
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private HashMap<String, ObjectNode> getTemplates(Connection connection) throws SQLException,
            JsonProcessingException {
        HashMap<String, ObjectNode> templates = new HashMap<>();
        String command = "SELECT id, template FROM templates";

        ResultSet rs = connection.createStatement().executeQuery(command);
        while (rs.next()) {
            templates.put(rs.getString("id"),
                    (ObjectNode) new ObjectMapper().readTree(rs.getString("template")));
        }

        return templates;
    }
}
