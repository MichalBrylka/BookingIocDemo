package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;
import an.awesome.pipelinr.Pipelinr;
import io.javalin.Javalin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "pipelines")
class IoC {
    @Bean
    public Pipelinr pipelinr(org.springframework.context.ApplicationContext context) {
        return new Pipelinr().with(() -> context.getBeansOfType(Command.Handler.class).values().stream());
    }

    @Bean
    public Pipelinr pipeline(ObjectProvider<Command.Handler> commandHandlers, ObjectProvider<Command.Middleware> middlewares
                             //,ObjectProvider<Notification.Handler> notificationHandlers
    ) {
        return new Pipelinr()
                .with(() -> commandHandlers.stream())
                .with(() -> middlewares.orderedStream())
                //.with(notificationHandlers::stream)
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
