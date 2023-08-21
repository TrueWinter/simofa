package dev.truewinter.simofa.formvalidators;

import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;

import java.util.Optional;

public class AddEditGitValidator implements FormValidator {
    @Override
    public Optional<String> hasError(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (Util.isBlank(username) || Util.isBlank(password)) {
            return Optional.of("All fields are required");
        }

        return Optional.empty();
    }
}
