package dev.truewinter.simofa;

import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpResponseException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;

public class RouteLoader {
    @SuppressWarnings("FieldCanBeLocal")
    private final String ROUTES_LOCATION = "dev.truewinter.simofa.routes";

    public void load(Javalin server) throws Exception {
        // Ignore RouteLoaderException which gets
        // thrown if a request fails verification
        server.exception(RouteLoaderException.class, (exception, context) -> {});

        for (Class<? extends Route> route : getRoutes(ROUTES_LOCATION)) {
            if (!route.isAnnotationPresent(RouteClass.class)) continue;
            RouteClass routeClass = route.getAnnotation(RouteClass.class);
            Route routeInstance = route.getDeclaredConstructor().newInstance();
            boolean registeredBefore = false;

            for (Method method : route.getMethods()) {
                if (!method.isAnnotationPresent(RouteInfo.class)) continue;
                if (method.getParameterCount() != 1) {
                    throw new Exception("Failed to load route [" + method.getName() + "] " + route.getSimpleName() + ". Method has too many parameters.");
                }

                try {
                    Class<?> firstParam = method.getParameterTypes()[0];
                    firstParam.asSubclass(Context.class);

                    RouteInfo routeInfo = method.getAnnotation(RouteInfo.class);

                    // TODO: Remove this
                    if (!routeInfo.url().startsWith("/api") && !routeInfo.url().startsWith("/public-api")) continue;

                    if (!registeredBefore) {
                        if (routeClass.verifyLogin()) {
                            server.before(routeInfo.url(), ctx -> {
                                if (routeClass.verifyLogin()) {
                                    if (!Route.verifyLogin(ctx)) {
                                        throw new RouteLoaderException();
                                    }
                                }
                            });
                        }

                        registeredBefore = true;
                    }

                    server.addHandler(routeInfo.method(), routeInfo.url(), ctx -> {
                        try {
                            method.invoke(routeInstance, ctx);
                        } catch (InvocationTargetException e) {
                            if (e.getTargetException() instanceof HttpResponseException) {
                                throw (HttpResponseException) e.getTargetException();
                            }

                            throw e;
                        }
                    });

                    Simofa.getLogger().info(
                            String.format("Registered route %s %s", routeInfo.method(), routeInfo.url())
                    );
                } catch (Exception e) {
                    throw new Exception("Failed to load route " + route.getSimpleName(), e);
                }
            }
        }
    }

    private List<Class<? extends Route>> getRoutes(String routesLocation) throws Exception {
        final String ROUTES_LOCATION_FS = routesLocation.replace(".", "/");
        List<Class<? extends Route>> routes = new ArrayList<>();
        URL pkg = getClass().getClassLoader().getResource(ROUTES_LOCATION_FS);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream) Objects.requireNonNull(pkg).getContent()));

        List<String> routeClasses = new ArrayList<>();
        if (pkg.getProtocol().equals("jar")) {
            String jarFileName = URLDecoder.decode(pkg.getFile(), StandardCharsets.UTF_8);
            jarFileName = jarFileName.substring(5,jarFileName.indexOf("!"));

            try (JarFile jarFile = new JarFile(jarFileName)) {
                jarFile.entries().asIterator().forEachRemaining(j -> {
                    if (j.getName().startsWith(ROUTES_LOCATION_FS)) {
                        String c = j.getName()
                                .replace(ROUTES_LOCATION_FS, "")
                                .replaceFirst("^/", "");
                        if (!c.isBlank()) {
                            routeClasses.add(c);
                        }
                    }
                });
            }
        } else {
            String className;
            while ((className = bufferedReader.readLine()) != null) {
                routeClasses.add(className);
            }
        }

        for (String className : routeClasses) {
            String routeName = className.replaceFirst("\\.class$", "")
                    .replace("/", ".");

            // Sub-classes in the same file
            if (routeName.contains("$")) {
                continue;
            }

            // Packages
            if (!className.endsWith(".class")) {
                if (!pkg.getProtocol().equals("jar")) {
                   routes.addAll(getRoutes(routesLocation + "." + className
                            .replace("/", ".")
                            .replaceFirst("\\.$", "")
                    ));
                }

                continue;
            }

            try {
                URLClassLoader urlClassLoader = new URLClassLoader(
                        new URL[]{pkg},
                        getClass().getClassLoader()
                );
                Class<? extends Route> route = getPluginAsSubclass(urlClassLoader, routesLocation + "." + routeName);
                routes.add(route);
            } catch (Exception ignored) {}
        }

        return routes;
    }

    private Class<? extends Route> getPluginAsSubclass(URLClassLoader classLoader, String routeClass) throws Exception {
        return Class.forName(routeClass, false, classLoader).asSubclass(Route.class);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    public @interface RouteInfo {
        String url();
        HandlerType method() default HandlerType.GET;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface RouteClass {
        boolean verifyLogin() default true;
    }

    private static class RouteLoaderException extends Exception {}
}
