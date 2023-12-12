package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.Account;
import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeleteAccountRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/accounts/{id}/delete",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));

        try {
            Optional<Account> account = getDatabase().getAccountDatabase().getAccountById(id);
            account.ifPresentOrElse(a -> {
                try {
                    getDatabase().getAccountDatabase().deleteAccount(id);
                    redirect(ctx,"/accounts");
                } catch (SQLException e) {
                    e.printStackTrace();
                    ctx.status(500).result("Failed to delete account");
                }
            }, () -> {
                ctx.status(404).result("Account does not exist");
            });
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).result("An error occurred: " + e.getMessage());
        }
    }
}
