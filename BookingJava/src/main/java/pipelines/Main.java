package pipelines;

import an.awesome.pipelinr.*;

import java.util.*;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        AppComponent app = DaggerAppComponent.create();

        Javalin javalin = Javalin.create(config -> {
            config.jsonMapper(new JacksonJsonMapper());
            config.bundledPlugins.enableDevLogging();
        }).start(8080);

        new BookingController(app.pipeline()).registerRoutes(javalin);

        System.out.println("🚀 Server running at http://localhost:8080");
    }
}
