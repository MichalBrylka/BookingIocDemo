package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;
import an.awesome.pipelinr.Pipelinr;
import io.javalin.Javalin;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import pipelines.controllers.BookingController;

import static pipelines.infrastructure.WebAppCreator.createJavalinApp;

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
}


