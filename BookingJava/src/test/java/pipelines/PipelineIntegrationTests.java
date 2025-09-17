package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Pipelinr;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineIntegrationTests {
    private Pipelinr pipelinr;


    @BeforeEach
    void setup() {
        LinkedHashMap<@NotNull UUID, @NotNull Booking> bookings = Stream.of(
                new Booking(new UUID(0L, 1L), "Hotel California", "Alice Smith", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 5)),
                new Booking(new UUID(0L, 2L), "Grand Budapest", "Bob Johnson", LocalDate.of(2024, 8, 10), LocalDate.of(2024, 8, 15)),
                new Booking(new UUID(0L, 3L), "The Overlook", "Charlie Brown", LocalDate.of(2024, 9, 20), LocalDate.of(2024, 9, 22))
        ).collect(Collectors.toMap(
                Booking::id,
                booking -> booking,
                (existing, replacement) -> existing,
                LinkedHashMap::new
        ));

        var repository = new InMemoryBookingRepository(bookings) {
            private final AtomicLong counter = new AtomicLong(10L);

            @Override
            public UUID getNextId() {
                return new UUID(0L, counter.getAndIncrement());
            }
        };

        pipelinr = TestPipeline.create(repository);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelineTestCases")
    <R> void testPipeline(Command<R> commandOrQuery, R expectedResult) {
        R result = pipelinr.send(commandOrQuery);

        switch (expectedResult) {
            case Boolean bool -> assertThat(result).isEqualTo(bool);
            case Integer i -> assertThat((Iterable<?>) result).hasSize(i);
            case null -> assertThat(result).isNull();
            default -> assertThat(result).isEqualTo(expectedResult);
        }
    }

    static Stream<Arguments> pipelineTestCases() {
        UUID existingId = new UUID(0L, 1L);
        UUID nonExistingId = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);

        LocalDate today = LocalDate.now();

        return Stream.of(
                Arguments.of(Named.of("Book hotel command", new BookHotelCommand("HotelX", "Eve", today, today.plusDays(2))), new UUID(0L, 10L)),

                Arguments.of(Named.of("Get existing booking by id", new GetBookingsByIdQuery(existingId)),
                        new Booking(new UUID(0L, 1L), "Hotel California", "Alice Smith", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 5))
                ),
                Arguments.of(Named.of("Get non-existing booking by id", new GetBookingsByIdQuery(nonExistingId)), null),

                Arguments.of(Named.of("Get all bookings", new GetBookingsQuery(Optional.empty(), Optional.empty())), 3),
                Arguments.of(Named.of("Get bookings filtered by hotel name", new GetBookingsQuery(Optional.of("Hotel California"), Optional.empty())), 1),

                Arguments.of(Named.of("Update booking", new UpdateBookingCommand(existingId, "UpdatedHotel", "Eve", today, today.plusDays(3))), true),

                Arguments.of(Named.of("Patch booking", new PatchBookingCommand(existingId, Map.of("guestName", "Frank"))), true),

                Arguments.of(Named.of("Delete booking", new DeleteBookingCommand(existingId)), true)
        );
    }
}
