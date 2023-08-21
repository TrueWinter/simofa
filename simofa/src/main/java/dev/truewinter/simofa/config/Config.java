package dev.truewinter.simofa.config;

import net.william278.annotaml.YamlComment;
import net.william278.annotaml.YamlFile;
import net.william278.annotaml.YamlKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("FieldMayBeFinal")
@YamlFile()
public class Config {
    @YamlKey("port")
    private int port = 8808;

    @YamlKey("database.host")
    private String dbHost = "localhost";

    @YamlKey("database.port")
    private int dbPort = 3306;

    @YamlKey("database.database")
    private String database = "";

    @YamlKey("database.username")
    private String dbUsername = "";

    @YamlKey("database.password")
    private String dbPassword = "";

	@YamlKey("database.connection.properties")
    private String dbConnectionProperties = "?autoReconnect=true";

    @YamlComment("Secret used for signing JWTs")
    @YamlKey("secret")
    private String secret = "simofa";

    @YamlComment("Simofa URL, must be reachable from deployment servers")
    @YamlKey("url")
    private String url = "http://localhost:8808";

    @YamlKey("concurrent_builds")
    private int concurrentBuilds = 2;

    @YamlKey("docker.host")
    private String dockerHost = "tcp://127.0.0.1:2375";

    @YamlKey("docker.verify_tls")
    private boolean dockerVerifyTls = false;

    @YamlComment("Optional")
    @YamlKey("docker.cert_path")
    private String dockerCertPath = "";

    @YamlComment("Docker Registry configuration, optional")
    @YamlKey("docker.registry")
    @SuppressWarnings("unused")
    // Doing it this way ensures the comment appears
    // in the correct place in the YAML file
    private Map<String, String> dockerRegistry = Map.of(
        "username", "",
        "password", "",
        "email", "",
        "url", ""
    );

    @YamlComment("Do not touch this if you're not a developer")
    @YamlKey("simofa_internals")
    private Map<String, Object> simofaInternals = Map.of(
            "dev", false
    );

    public int getPort() {
        return port;
    }

    public String getDbHost() {
        return dbHost;
    }

    public int getDbPort() {
        return dbPort;
    }

    public String getDatabase() {
        return database;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }
	
	public String getDbConnectionProperties() {
		return dbConnectionProperties;
	}

    public String getSecret() {
        return secret;
    }

    public String getUrl() {
        return url;
    }

    public int getConcurrentBuilds() {
        return concurrentBuilds;
    }

    public String getDockerHost() {
        return dockerHost;
    }

    public boolean shouldDockerVerifyTls() {
        return dockerVerifyTls;
    }

    public String getDockerCertPath() {
        return dockerCertPath;
    }

    public String getDockerUsername() {
        return dockerRegistry.get("username");
    }

    public String getDockerPassword() {
        return dockerRegistry.get("password");
    }

    public String getDockerEmail() {
        return dockerRegistry.get("email");
    }

    public String getDockerUrl() {
        return dockerRegistry.get("url");
    }

    public boolean isDevMode() {
        return (boolean) simofaInternals.get("dev");
    }
}
