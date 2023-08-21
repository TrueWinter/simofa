package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AccountDatabase {
    private HikariDataSource ds;

    public AccountDatabase(HikariDataSource ds) {
        this.ds = ds;
    }

    public void addAccount(String username, String password) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?);");
            statement.setString(1, username);
            statement.setString(2, Account.createHash(password));
            statement.execute();
        }
    }

    public Optional<Account> getAccountByUsername(String username) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE username = ?;");
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Account account = new Account(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                return Optional.of(account);
            } else {
                return Optional.empty();
            }
        }
    }

    public Optional<Account> getAccountById(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE id = ?;");
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                Account account = new Account(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                return Optional.of(account);
            } else {
                return Optional.empty();
            }
        }
    }

    public Optional<Account> getAccountIfPasswordIsCorrect(String username, String password) throws SQLException {
        return getAccountByUsername(username).filter(a -> Account.isCorrectPassword(password, a));
    }

    public List<Account> getAccounts() throws SQLException {
        List<Account> accounts = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users`;");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Account account = new Account(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password")
                );
                accounts.add(account);
            }
        }

        return accounts;
    }

    public void deleteAccount(int id) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `users` WHERE id = ?;");
            statement.setInt(1, id);
            statement.execute();
        }
    }

    public void editAccount(Account account) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("UPDATE `users` SET " +
                    "username = ?, " +
                    "password = ? " +
                    "WHERE id = ?;"
            );

            statement.setString(1, account.getUsername());
            statement.setString(2, account.getPasswordHash());
            statement.setInt(3, account.getId());
            statement.execute();
        }
    }
}
