package dev.truewinter.simofa;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Account {
    private final int id;
    private final String username;
    @JsonIgnore
    private final String passwordHash;

    public Account(int id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public static String createHash(String password) {
        return BCrypt.with(BCrypt.Version.VERSION_2A, LongPasswordStrategies.none()).hashToString(10, password.toCharArray());
    }

    public static boolean isCorrectPassword(String password, Account account) {
        return BCrypt.verifyer(BCrypt.Version.VERSION_2A, LongPasswordStrategies.none()).verify(password.toCharArray(), account.getPasswordHash().toCharArray()).verified;
    }
}
