package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.Template;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TemplatesDatabase {
    private HikariDataSource ds;

    public TemplatesDatabase(HikariDataSource ds) {
        this.ds = ds;
    }

    public Optional<Template> getTemplateById(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `templates` WHERE id = ?;");
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Template template = new Template(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("template")
                );
                return Optional.of(template);
            } else {
                return Optional.empty();
            }
        }
    }

    public void addTemplate(Template template) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO templates (name, template) VALUES (?, ?);");
            statement.setString(1, template.getName());
            statement.setString(2, template.getTemplate());
            statement.execute();
        }
    }

    public void editTemplate(Template template) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `templates` SET " +
                    "name = ?, " +
                    "template = ? " +
                    "WHERE id = ?;"
            );

            statement.setString(1, template.getName());
            statement.setString(2, template.getTemplate());
            statement.execute();
        }
    }

    public List<Template> getTemplates() throws SQLException {
        List<Template> templates = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `templates`;");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Template template = new Template(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("template")
                );
                templates.add(template);
            }
        }

        return templates;
    }

    public void deleteTemplate(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `templates` WHERE id = ?;");
            statement.setInt(1, id);
            statement.execute();
        }
    }
}
