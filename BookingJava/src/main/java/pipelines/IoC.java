package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;
import an.awesome.pipelinr.Pipeline;
import an.awesome.pipelinr.Pipelinr;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

@Configuration
@ComponentScan(basePackages = "pipelines")
class IoC {
    @Bean
    public Pipelinr pipeline(ObjectProvider<Command.Handler> commandHandlers,
                             ObjectProvider<Command.Middleware> middlewares,
                             ObjectProvider<Notification.Handler> notificationHandlers
    ) {
        return new Pipelinr()
                .with(() -> commandHandlers.stream())
                .with(() -> middlewares.orderedStream())
                .with(() -> notificationHandlers.stream())
                ;
    }


    @Bean
    public Javalin javalin(BookingController bookingController) {
        return createJavalinApp(bookingController, config -> config.bundledPlugins.enableDevLogging());
    }

    public static Javalin createJavalinApp(BookingController bookingController, Consumer<JavalinConfig> configBuilder) {
        var app = Javalin.create(config -> {
            config.router.apiBuilder(bookingController);
            config.jsonMapper(new JacksonJsonMapper());

            if (configBuilder != null) configBuilder.accept(config);
        });
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()))
        );
        return app;
    }
}

@Service
class NotificationPublisher {
    private final Pipeline pipeline;

    public NotificationPublisher(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void publish(Notification notification) {
        pipeline.send(notification);
    }
}
