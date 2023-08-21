package dev.truewinter.simofa.formvalidators;

import io.javalin.http.Context;

import java.util.Optional;

public interface FormValidator {
    Optional<String> hasError(Context ctx);
}
