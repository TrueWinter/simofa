package dev.truewinter.simofa.formvalidators;

import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class AddEditDeploymentServerValidator implements FormValidator {
    @Override
    public Optional<String> hasError(Context ctx) {
        String name = ctx.formParam("name");
        String url = ctx.formParam("url");
        String key = ctx.formParam("key");

        if (Util.isBlank(name) || Util.isBlank(url) || Util.isBlank(key)) {
            return Optional.of("All fields are required");
        }

        try {
            new URL(url);
        } catch (MalformedURLException e) {
            return Optional.of("Invalid URL");
        }

        return Optional.empty();
    }
}
