package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.Account;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class EditAccountRoute extends Route {
    private HashMap<String, Object> getAccountDataForModel(Account a) throws JsonProcessingException {
        // This configuration makes it easier to get the
        // relevant data in the Pebble template.
        HashMap<String, Object> containerMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String accountJson = objectMapper.writeValueAsString(a);
        @SuppressWarnings("unchecked")
        HashMap<Object, Object> accountData = objectMapper.readValue(accountJson, HashMap.class);

        containerMap.put("account", accountData);

        return containerMap;
    }

    @RouteLoader.RouteInfo(
            url = "/accounts/{id}/edit"
    )
    public void get(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<Account> account = getDatabase().getAccountDatabase().getAccountById(id);
            if (account.isEmpty()) {
                ctx.status(404).result("Account not found");
                return;
            }

            render(ctx, "accounts/edit", getAccountDataForModel(account.get()));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Failed to get account");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/accounts/{id}/edit",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<Account> account = getDatabase().getAccountDatabase().getAccountById(id);
            if (account.isEmpty()) {
                ctx.status(404).result("Account not found");
                return;
            }

            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            String confirm_password = ctx.formParam("confirm_password");

            if (Util.isBlank(username)) {
                renderError(ctx, "accounts/edit", "Username is required.", getAccountDataForModel(account.get()));
                return;
            }

            if (!Util.isBlank(password)) {
                if (Util.isBlank(confirm_password)) {
                    renderError(ctx, "accounts/edit", "You must confirm the password to change it.", getAccountDataForModel(account.get()));
                    return;
                }

                if (!password.equals(confirm_password)) {
                    renderError(ctx, "accounts/edit", "Password and password confirmation must be the same.", getAccountDataForModel(account.get()));
                    return;
                }
            }

            Account account1 = new Account(
                    account.get().getId(),
                    username,
                    getPasswordHash(password, account.get())
            );

            getDatabase().getAccountDatabase().editAccount(account1);
            renderSuccess(ctx, "accounts/edit", "Account edited.", getAccountDataForModel(account1));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Failed to get account");
        }
    }

    private String getPasswordHash(String password, Account account) {
        if (Util.isBlank(password)) {
            return account.getPasswordHash();
        }

        return Account.createHash(password);
    }
}
