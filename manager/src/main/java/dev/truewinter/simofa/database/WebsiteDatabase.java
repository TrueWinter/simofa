package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WebsiteDatabase {
    private HikariDataSource ds;

    public WebsiteDatabase(HikariDataSource ds) {
        this.ds = ds;
    }

    public Optional<Website> getWebsiteById(String id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT w.id, w.name, w.docker_image, w.memory, w.cpu, w.git_url, w.git_branch, w.git_credential, w.build_command, w.deployment_command, w.deployment_failed_command, w.deployment_server, w.deploy_token, git.id as gitId, git.username, git.password FROM `websites` AS w LEFT JOIN git ON w.git_credential = git.id WHERE w.id = ?;");
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Website website = new Website(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("docker_image"),
                        rs.getInt("memory"),
                        rs.getDouble("cpu"),
                        rs.getString("git_url"),
                        rs.getString("git_branch"),
                        rs.getString("git_credential"),
                        rs.getString("build_command"),
                        rs.getString("deployment_command"),
                        rs.getString("deployment_failed_command"),
                        rs.getString("deployment_server"),
                        rs.getString("deploy_token")
                );
                return Optional.of(website);
            } else {
                return Optional.empty();
            }
        }
    }

    public String addWebsite(Website website) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            String id = Util.createv7UUID().toString();

            PreparedStatement statement = connection.prepareStatement("INSERT INTO websites (id, name, docker_image, memory, cpu, git_url, git_branch, git_credential, build_command, deployment_command, deployment_failed_command, deployment_server, deploy_token) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
            statement.setString(1, id);
            statement.setString(2, website.getName());
            statement.setString(3, website.getDockerImage());
            statement.setInt(4, website.getMemory());
            statement.setDouble(5, website.getCpu());
            statement.setString(6, website.getGitUrl());
            statement.setString(7, website.getGitBranch());
            if (website.getGitCredentials() == null) {
                statement.setNull(8, Types.CHAR);
            } else {
                statement.setString(8, website.getGitCredentials());
            }
            statement.setString(9, website.getBuildCommand());
            statement.setString(10, website.getDeployCommand());
            statement.setString(11, website.getDeployFailedCommand());
            statement.setString(12, website.getDeployServer());
            statement.setString(13, website.getDeployToken());
            statement.execute();

            return id;
        }
    }

    public void editWebsite(Website website) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `websites` SET " +
                    "name = ?, " +
                    "docker_image = ?, " +
                    "memory = ?, " +
                    "cpu = ?, " +
                    "git_url = ?, " +
                    "git_branch = ?, " +
                    "git_credential = ?, " +
                    "build_command = ?, " +
                    "deployment_command = ?, " +
                    "deployment_failed_command = ?, " +
                    "deployment_server = ?, " +
                    "deploy_token = ? " +
                    "WHERE id = ?;"
            );

            statement.setString(1, website.getName());
            statement.setString(2, website.getDockerImage());
            statement.setInt(3, website.getMemory());
            statement.setDouble(4, website.getCpu());
            statement.setString(5, website.getGitUrl());
            statement.setString(6, website.getGitBranch());
            if (website.getGitCredentials() == null) {
                statement.setNull(7, Types.INTEGER);
            } else {
                statement.setString(7, website.getGitCredentials());
            }
            statement.setString(8, website.getBuildCommand());
            statement.setString(9, website.getDeployCommand());
            statement.setString(10, website.getDeployFailedCommand());
            statement.setString(11, website.getDeployServer());
            statement.setString(12, website.getDeployToken());
            statement.setString(13, website.getId());
            statement.execute();
        }
    }

    public List<Website> getWebsites() throws SQLException {
        List<Website> websites = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT w.id, w.name, w.docker_image, w.memory, w.cpu, w.git_url, w.git_branch, w.git_credential, w.build_command, w.deployment_command, w.deployment_failed_command, w.deployment_server, w.deploy_token, git.id as gitId, git.username, git.password FROM `websites` AS w LEFT JOIN git ON w.git_credential = git.id;");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Website website = new Website(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("docker_image"),
                        rs.getInt("memory"),
                        rs.getDouble("cpu"),
                        rs.getString("git_url"),
                        rs.getString("git_branch"),
                        rs.getString("git_credential"),
                        rs.getString("build_command"),
                        rs.getString("deployment_command"),
                        rs.getString("deployment_failed_command"),
                        rs.getString("deployment_server"),
                        rs.getString("deploy_token")
                );
                websites.add(website);
            }
        }

        return websites;
    }

    public void deleteWebsite(String id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `websites` WHERE id = ?;");
            statement.setString(1, id);
            statement.execute();
        }
    }
}
