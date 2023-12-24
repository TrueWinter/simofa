package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.api.GitCredential;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GitDatabase {
    private HikariDataSource ds;

    public GitDatabase(HikariDataSource ds) {
        this.ds = ds;
    }

    public Optional<GitCredential> getGitCredential(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `git` WHERE id = ?;");
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                GitCredential gitCredential = new GitCredential(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                return Optional.of(gitCredential);
            } else {
                return Optional.empty();
            }
        }
    }

    public void addGitCredential(GitCredential gitCredential) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `git` (username, password) VALUES (?, ?);");
            statement.setString(1, gitCredential.getUsername());
            statement.setString(2, gitCredential.getPassword());
            statement.execute();
        }
    }

    public void editGitCredential(GitCredential gitCredential) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `git` SET " +
                    "username = ?, " +
                    "password = ? " +
                    "WHERE id = ?;"
            );

            statement.setString(1, gitCredential.getUsername());
            statement.setString(2, gitCredential.getPassword());
            statement.setInt(3, gitCredential.getId());
            statement.execute();
        }
    }

    public List<GitCredential> getGitCredentials() throws SQLException {
        List<GitCredential> gitCredentials = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `git`;");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                GitCredential gitCredential = new GitCredential(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                gitCredentials.add(gitCredential);
            }
        }

        return gitCredentials;
    }

    public void deleteGitCredential(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `git` WHERE id = ?;");
            statement.setInt(1, id);
            statement.execute();
        }
    }
}
