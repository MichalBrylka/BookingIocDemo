package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Pipeline;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PipelineIntegrationTests {
    private Pipeline pipeline;
    private EmailService emailService;


    @BeforeEach
    void setup() {
        LinkedHashMap<@NotNull UUID, @NotNull Booking> bookings = TestingInfrastructure.getExampleBookings();

        var repository = new InMemoryBookingRepository(bookings) {
            private final AtomicLong counter = new AtomicLong(10L);

            @Override
            public UUID getNextId() {
                return new UUID(0L, counter.getAndIncrement());
            }
        };

        emailService = Mockito.mock(EmailService.class);

        pipeline = TestingInfrastructure.createPipeline(repository, emailService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("pipelineTestCases")
    <R> void testPipeline(Command<R> commandOrQuery, R expectedResult, String emailToSend) {
        R result = pipeline.send(commandOrQuery);

        if (emailToSend == null)
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        else verify(emailService, times(1)).sendEmail(eq(emailToSend), anyString(), anyString());

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
                Arguments.of(Named.of("Book hotel command", new BookHotelCommand("HotelX", "Eve", "eve@wp.pl", today, today.plusDays(2))), new UUID(0L, 10L), "eve@wp.pl"),

                Arguments.of(Named.of("Get existing booking by id", new GetBookingsByIdQuery(existingId)),
                        new Booking(new UUID(0L, 1L), "Hotel California", "Alice Smith", "alice.beauty@buziaczek.pl", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 5))
                        , null),
                Arguments.of(Named.of("Get non-existing booking by id", new GetBookingsByIdQuery(nonExistingId)), null, null),

                Arguments.of(Named.of("Get all bookings", new GetBookingsQuery(null, null)), 3, null),
                Arguments.of(Named.of("Get bookings filtered by hotel name",
                        new GetBookingsQuery(new BookingFilter(new BookingFilter.StringFilter("Hotel California", BookingFilter.Operator.EQ), null, null, null, null), null)
                ), 1, null),

                Arguments.of(Named.of("Update booking", new UpdateBookingCommand(existingId, "UpdatedHotel", "Eve", "new@email.pl", today, today.plusDays(3))), true, null),

                Arguments.of(Named.of("Patch booking", new PatchBookingCommand(existingId, Map.of("guestName", "Frank"))), true, null),

                Arguments.of(Named.of("Delete booking", new DeleteBookingCommand(existingId)), true, null)
        );
    }
}
