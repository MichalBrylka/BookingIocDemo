package pipelines;

import an.awesome.pipelinr.*;
import dagger.Module;
import dagger.Provides;
import dagger.Component;

import javax.inject.Singleton;
import java.util.List;

@Module
class AppModule {
    @Provides
    @Singleton
    BookingRepository repository() {
        return new InMemoryBookingRepository();
    }

    @Provides
    @Singleton
    BookHotelHandler provideBookHotelHandler(BookingRepository repo) {
        return new BookHotelHandler(repo);
    }

    @Provides
    @Singleton
    GetBookingsHandler provideGetBookingsHandler(BookingRepository repo) {
        return new GetBookingsHandler(repo);
    }

    @Provides
    @Singleton
    List<Command.Handler> provideHandlers(
            BookHotelHandler bookHandler,
            GetBookingsHandler getHandler
    ) {
        return List.of(bookHandler, getHandler);
    }

    @Provides
    @Singleton
    Pipeline providePipeline(List<Command.Handler> handlers) {
        return new Pipelinr().with(() -> handlers.stream());
    }


   /* @Provides
    @Singleton
    Pipeline pipeline(List<Command.Handler> handlers, List<Command.Middleware> middlewares) {
        return new Pipelinr()
                .with(handlers::stream)
                .with(middlewares::stream);
    }*/
}


@Singleton
@Component(modules = AppModule.class)
interface AppComponent {
    BookingRepository repository();

    Pipeline pipeline();
}