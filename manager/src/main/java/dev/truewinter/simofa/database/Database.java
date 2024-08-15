package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public class Database {
    private HikariDataSource ds;
    private AccountDatabase accountDatabase;
    private WebsiteDatabase websiteDatabase;
    private DeployServerDatabase deployServerDatabase;
    private TemplatesDatabase templatesDatabase;
    private GitDatabase gitDatabase;

    public Database(Config config) {
        if (config.getDbUsername().isBlank() && config.getDbPassword().isBlank()) {
            Simofa.getLogger().warn("Database not configured: username and password blank");
            System.exit(0);
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getDbHost() +
                ":" + config.getDbPort() + "/" + config.getDatabase()
				+ config.getDbConnectionProperties());
        hikariConfig.setUsername(config.getDbUsername());
        hikariConfig.setPassword(config.getDbPassword());

        // https://github.com/WiIIiam278/HuskTowns/blob/master/common/src/main/java/net/william278/husktowns/database/MySqlDatabase.java
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setKeepaliveTime(30000);
        hikariConfig.setConnectionTimeout(20000);

        // Set additional connection pool properties
        hikariConfig.setDataSourceProperties(new Properties() {{
            put("cachePrepStmts", "true");
            put("prepStmtCacheSize", "250");
            put("prepStmtCacheSqlLimit", "2048");
            put("useServerPrepStmts", "true");
            put("useLocalSessionState", "true");
            put("useLocalTransactionState", "true");
            put("rewriteBatchedStatements", "true");
            put("cacheResultSetMetadata", "true");
            put("cacheServerConfiguration", "true");
            put("elideSetAutoCommits", "true");
            put("maintainTimeStats", "false");
        }});

        ds = new HikariDataSource(hikariConfig);

        this.accountDatabase = new AccountDatabase(ds);
        this.websiteDatabase = new WebsiteDatabase(ds);
        this.deployServerDatabase = new DeployServerDatabase(ds);
        this.templatesDatabase = new TemplatesDatabase(ds);
        this.gitDatabase = new GitDatabase(ds);

        try (Connection connection = ds.getConnection()) {
            for (String statement : getInitSql()) {
                connection.createStatement().execute(statement);
            }

            if (accountDatabase.getAccountCount() == 0) {
                Simofa.getLogger().info("Default admin account does not exist, creating it...");
                accountDatabase.addAccount("admin", "simofa");
                Simofa.getLogger().info("Default admin account created with username `admin` and password `simofa`. Please change it to something more secure.");
            }
        } catch (SQLException e) {
            Simofa.getLogger().warn("Failed to initialize database", e);
        }
    }

    private String[] getInitSql() {
        try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("database/mysql_init.sql")) {
            String initSql = new String(Objects.requireNonNull(stream).readAllBytes());
            return initSql.split(";");
        } catch (IOException e) {
            Simofa.getLogger().warn("Failed to load initial SQL statements", e);
        }

        return new String[0];
    }

    public void close() {
        ds.close();
    }

    public Connection _getConnection() throws SQLException {
        return ds.getConnection();
    }

    public AccountDatabase getAccountDatabase() {
        return accountDatabase;
    }

    public WebsiteDatabase getWebsiteDatabase() {
        return websiteDatabase;
    }

    public DeployServerDatabase getDeployServerDatabase() {
        return deployServerDatabase;
    }

    public TemplatesDatabase getTemplatesDatabase() {
        return templatesDatabase;
    }

    public GitDatabase getGitDatabase() {
        return gitDatabase;
    }
}
