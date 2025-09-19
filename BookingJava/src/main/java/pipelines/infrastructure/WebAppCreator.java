package pipelines.infrastructure;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import pipelines.controllers.BookingController;

import java.util.Map;
import java.util.function.Consumer;

public class WebAppCreator {
    public static Javalin createJavalinApp(BookingController bookingController, Consumer<JavalinConfig> configBuilder) {
        var app = Javalin.create(config -> {

            config.registerPlugin(new OpenApiPlugin(pluginConfig ->
                    pluginConfig.withDefinitionConfiguration((version, definition) ->
                            definition.withInfo(info -> {
                                info.setTitle("Booking OpenAPI example with Javalin");
                                info.setDescription("API documentation for Booking application");
                                info.setVersion("1.0.0");
                            })
                    )
            ));

            config.registerPlugin(new SwaggerPlugin());
            config.registerPlugin(new ReDocPlugin());

            config.router.apiBuilder(bookingController);
            config.jsonMapper(new JacksonJsonMapper());

            if (configBuilder != null) configBuilder.accept(config);
        });
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()))
        );

        app.get("/", ctx -> ctx.redirect("/swagger/?url=/openapi"));
        return app;
    }
}
