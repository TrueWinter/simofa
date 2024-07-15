package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Account;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class AccountsAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/accounts"
    )
    public void get(Context ctx) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("accounts", new ArrayList<>());

        try {
            List<Account> accounts = getDatabase().getAccountDatabase().getAccounts();
            resp.put("accounts", accounts);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
        }

        ctx.json(resp);
    }
}
