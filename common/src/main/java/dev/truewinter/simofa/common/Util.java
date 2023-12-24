package dev.truewinter.simofa.common;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public class Util {
    // https://stackoverflow.com/a/15954821
    public static String getInstallPath() {
        Path relative = Paths.get("");
        return relative.toAbsolutePath().toString();
    }

    public static String getVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(Util.class.getClassLoader().getResourceAsStream("properties/simofa.properties"));
        return properties.getProperty("version");
    }

    // https://stackoverflow.com/a/50381020
    public static String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(bytes);
    }

    public static boolean isBlank(@Nullable String string) {
        if (string == null) return true;
        return string.isBlank();
    }

    // https://security.stackexchange.com/a/83671
    public static boolean secureCompare(String s1, String s2) {
        byte[] s1b = s1.getBytes();
        byte[] s2b = s2.getBytes();

        // Technically this is vulnerable to a timing attack,
        // but simply knowing the length shouldn't give the
        // attacker much information about the value of the
        // strings if they are securely and randomly generated.
        if (s1b.length != s2b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < s1b.length; i++) {
            result |= s1b[i] ^ s2b[i];
        }

        return result == 0;
    }

    public static boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static File createTempDir(String dirName) throws Exception {
        File tempDir = new File(getTempDirPath(dirName));
        if (tempDir.exists()) {
            System.out.printf("Temporary directory %s already exists, deleting it%n", tempDir);
            FileUtils.deleteDirectory(tempDir);
        }

        if (!tempDir.mkdir()) {
            throw new Exception("Failed to create temporary directory " + tempDir);
        }

        File tmpInDir = new File(tempDir, "in");
        File tmpOutDir = new File(tempDir, "out");
        File tmpScriptsDir = new File(tempDir, "scripts");
        File tmpCacheDir = new File(tempDir, "cache");

        if (!(tmpInDir.mkdir() && tmpOutDir.mkdir() && tmpScriptsDir.mkdir() && tmpCacheDir.mkdir())) {
            throw new Exception("Failed to create temporary directory " + tempDir);
        }

        return tempDir;
    }

    public static String getTempDirPath(String dirname) {
        return new File(System.getProperty("java.io.tmpdir"), getTempDirName(dirname)).getAbsolutePath();
    }

    public static String getTempDirName(String dirName) {
        return "simofa-" + dirName;
    }

    @Nullable
    public static String base64Decode(String b64) {
        try {
            return new String(Base64.getDecoder().decode(b64));
        } catch (Exception e) {
            return null;
        }
    }

    public static String base64Encode(String string) {
        return new String(Base64.getEncoder().encode(string.getBytes(StandardCharsets.UTF_8)));
    }

    public static String dos2unix(String string) {
        return string.replaceAll("\r\n", "\n");
    }

    public static List<File> getPluginJars() throws Exception {
        List<File> pluginJars = new ArrayList<>();

        File[] files = Path.of(getInstallPath(), "plugins").toFile()
                .listFiles((dir, name) -> name.endsWith(".jar"));

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    pluginJars.add(file);
                }
            }
        }

        return pluginJars;
    }
}
