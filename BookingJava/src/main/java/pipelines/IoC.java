package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;
import an.awesome.pipelinr.Pipeline;
import an.awesome.pipelinr.Pipelinr;
import io.javalin.Javalin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

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
    public Javalin javalin(JacksonJsonMapper jacksonJsonMapper, BookingController bookingController) {
        var app = Javalin.create(config -> {
            config.jsonMapper(jacksonJsonMapper);
            config.bundledPlugins.enableDevLogging();
        });
        bookingController.registerRoutes(app);
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
