package pipelines;

import an.awesome.pipelinr.*;
import org.jetbrains.annotations.NotNull;
import pipelines.data.BookingRepository;
import pipelines.domain.Booking;
import pipelines.handlers.*;
import pipelines.infrastructure.EmailService;
import pipelines.infrastructure.NotificationPublisher;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TestingInfrastructure {
    static Pipeline createPipeline(BookingRepository repository, EmailService emailService) {
        Pipelinr pipeline = new Pipelinr();

        var publisher = new NotificationPublisher(pipeline);

        pipeline = pipeline.with(() -> Stream.of(
                        new BookHotelHandler(repository, publisher),
                        new GetBookingsHandler(repository),
                        new DeleteBookingsHandler(repository),
                        new UpdateBookingHandler(repository),
                        new PatchBookingHandler(repository),
                        new GetBookingsByIdHandler(repository)
                ))
                .with(() -> Stream.of(
                        new SendConfirmationEmailHandler(emailService),
                        new UpdateInventoryHandler()
                ));

        return pipeline;
    }

    static LinkedHashMap<@NotNull UUID, @NotNull Booking> getExampleBookings() {
        return Stream.of(
                new Booking(new UUID(0L, 1L), "Hotel California", "Alice Smith", "alice.beauty@buziaczek.pl", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 5)),
                new Booking(new UUID(0L, 2L), "Grand Budapest", "Bob Johnson", "bob.johnson@gmail.com", LocalDate.of(2024, 8, 10), LocalDate.of(2024, 8, 15)),
                new Booking(new UUID(0L, 3L), "The Overlook", "Charlie Brown", "call@me.daddy", LocalDate.of(2024, 9, 20), LocalDate.of(2024, 9, 22))
        ).collect(Collectors.toMap(
                Booking::id,
                booking -> booking,
                (existing, replacement) -> existing,
                LinkedHashMap::new
        ));
    }
}
