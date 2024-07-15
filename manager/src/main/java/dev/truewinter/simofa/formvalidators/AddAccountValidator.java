package dev.truewinter.simofa.formvalidators;

import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;

import java.util.Optional;

public class AddAccountValidator implements FormValidator {
    @Override
    public Optional<String> hasError(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String confirmPassword = ctx.formParam("confirm_password");

        if (Util.isBlank(username) || Util.isBlank(password) || Util.isBlank(confirmPassword)) {
            return Optional.of("All fields are required");
        }

        if (!password.equals(confirmPassword)) {
            return Optional.of("Passwords must match");
        }

        return Optional.empty();
    }
}
