package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.api.DeploymentServer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DeploymentServerDatabase {
    private HikariDataSource ds;

    public DeploymentServerDatabase(HikariDataSource ds) {
        this.ds = ds;
    }

    public Optional<DeploymentServer> getDeploymentServer(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `deployment_servers` WHERE id = ?;");
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                DeploymentServer deploymentServer = new DeploymentServer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("url"),
                        rs.getString("key")
                );
                return Optional.of(deploymentServer);
            } else {
                return Optional.empty();
            }
        }
    }

    public void addDeploymentServer(DeploymentServer deploymentServer) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `deployment_servers` (name, url, `key`) VALUES (?, ?, ?);");
            statement.setString(1, deploymentServer.getName());
            statement.setString(2, deploymentServer.getUrl());
            statement.setString(3, deploymentServer.getKey());
            statement.execute();
        }
    }

    public void editDeploymentServer(DeploymentServer deploymentServer) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `deployment_servers` SET " +
                    "name = ?, " +
                    "url = ?, " +
                    "`key` = ? " +
                    "WHERE id = ?;"
            );

            statement.setString(1, deploymentServer.getName());
            statement.setString(2, deploymentServer.getUrl());
            statement.setString(3, deploymentServer.getKey());
            statement.setInt(4, deploymentServer.getId());
            statement.execute();
        }
    }

    public List<DeploymentServer> getDeploymentServers() throws SQLException {
        List<DeploymentServer> deploymentServers = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `deployment_servers`;");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                DeploymentServer deploymentServer = new DeploymentServer(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("url"),
                        rs.getString("key")
                );
                deploymentServers.add(deploymentServer);
            }
        }

        return deploymentServers;
    }

    public void deleteDeploymentServer(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `deployment_servers` WHERE id = ?;");
            statement.setInt(1, id);
            statement.execute();
        }
    }
}
