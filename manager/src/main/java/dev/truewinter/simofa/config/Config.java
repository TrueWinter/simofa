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

    @YamlComment("GitHub App secret")
    @YamlKey("github_app_secret")
    private String githubAppSecret = "simofa";

    @YamlComment("Domain (or IP and port), must be reachable from the internet")
    @YamlKey("remote_url.domain")
    private String remoteUrlDomain = "localhost:8808";

    @YamlKey("remote_url.secure")
    private boolean remoteUrlSecure = false;

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

    @YamlComment("Directory where website cache data will be stored, must be absolute and exclusively usable by Simofa")
    @YamlKey("cache_directory")
    private String cacheDir;

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

    public String getGithubAppSecret() {
        return githubAppSecret;
    }

    public String getRemoteUrlDomain() {
        return remoteUrlDomain;
    }

    public boolean isRemoteUrlSecure() {
        return remoteUrlSecure;
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

    public String getCacheDir() {
        return cacheDir;
    }

    public void disableCache() {
        cacheDir = null;
    }
}
