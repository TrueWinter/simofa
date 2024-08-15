package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.api.DeployServer;
import dev.truewinter.simofa.common.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DeployServerDatabase {
    private final HikariDataSource ds;

    public DeployServerDatabase(HikariDataSource ds) {
        this.ds = ds;
    }

    public Optional<DeployServer> getDeployServer(String id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `deployment_servers` WHERE id = ?;");
            statement.setString(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                DeployServer deployServer = new DeployServer(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("url"),
                        rs.getString("key")
                );
                return Optional.of(deployServer);
            } else {
                return Optional.empty();
            }
        }
    }

    public String addDeployServer(DeployServer deployServer) throws SQLException {
        String uuid = Util.createv7UUID().toString();
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `deployment_servers` (id, name, url, `key`) VALUES (?, ?, ?, ?);");
            statement.setString(1, uuid);
            statement.setString(2, deployServer.getName());
            statement.setString(3, deployServer.getUrl());
            statement.setString(4, deployServer.getKey());
            statement.execute();
        }
        return uuid;
    }

    public void editDeployServer(DeployServer deployServer) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `deployment_servers` SET " +
                    "name = ?, " +
                    "url = ?, " +
                    "`key` = ? " +
                    "WHERE id = ?;"
            );

            statement.setString(1, deployServer.getName());
            statement.setString(2, deployServer.getUrl());
            statement.setString(3, deployServer.getKey());
            statement.setString(4, deployServer.getId());
            statement.execute();
        }
    }

    public List<DeployServer> getDeployServers() throws SQLException {
        List<DeployServer> deployServers = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `deployment_servers`;");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                DeployServer deployServer = new DeployServer(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("url"),
                        rs.getString("key")
                );
                deployServers.add(deployServer);
            }
        }

        return deployServers;
    }

    public void deleteDeployServer(String id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `deployment_servers` WHERE id = ?;");
            statement.setString(1, id);
            statement.execute();
        }
    }
}
