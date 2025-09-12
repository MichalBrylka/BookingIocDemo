package pipelines;

import an.awesome.pipelinr.*;

import java.util.*;

import io.javalin.Javalin;

public class Main {
    public static void main(String[] args) {
        BookingRepository repository = new InMemoryBookingRepository();

        List<Command.Handler> handlers = List.of(
                new BookHotelHandler(repository),
                new GetBookingsHandler(repository)
        );

        Pipelinr pipeline = new Pipelinr().with(() -> handlers.stream());


        var app = Javalin.create(config -> {
            config.jsonMapper(new JacksonJsonMapper());
            config.bundledPlugins.enableDevLogging();
        }).start(8080);


        new BookingController(pipeline).registerRoutes(app);
        System.out.println("🚀 Server running at http://localhost:" + app.port());
    }
}
