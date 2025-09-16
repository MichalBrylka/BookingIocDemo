package pipelines;

import an.awesome.pipelinr.Pipelinr;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

class BookingControllerTest {
    private Pipelinr pipelinr;
    private List<@NotNull Booking> bookings;
    private Javalin app;

    @BeforeEach
    void setup() {
        bookings = new ArrayList<>(List.of(
                new Booking(new UUID(0L, 1L), "Hotel California", "Alice Smith", LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 5)),
                new Booking(new UUID(0L, 2L), "Grand Budapest", "Bob Johnson", LocalDate.of(2024, 8, 10), LocalDate.of(2024, 8, 15)),
                new Booking(new UUID(0L, 3L), "The Overlook", "Charlie Brown", LocalDate.of(2024, 9, 20), LocalDate.of(2024, 9, 22))
        ));

        var repository = new InMemoryBookingRepository(bookings);

        pipelinr = new Pipelinr().with(() -> Stream.of(new BookHotelHandler(repository), new GetBookingsHandler(repository), new DeleteBookingsHandler(repository), new UpdateBookingHandler(repository), new PatchBookingHandler(repository)));
        var controller = new BookingController(pipelinr);
        app = Javalin.create(config -> config.showJavalinBanner = false);
        controller.registerRoutes(app);
    }

    @Test
    void testCreateBooking() {
        JavalinTest.test(app, (server, client) -> {
            var booking = Map.of("hotelName", "TestHotel", "guestName", "John Doe", "checkIn", "2024-07-01", "checkOut", "2024-07-05");
            try (var postRes = client.post("/bookings", booking)) {
                assertThat(postRes.code()).isEqualTo(200);
                assertThat(postRes.body()).isNotNull();
                assertThatJson(postRes.body().string()).node("bookingId").isEqualTo(bookings.getLast().id().toString());
            }
        });
    }

    @Test
    void testGetBooking() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.get("/bookings?hotelName=Hotel California")) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body()).isNotNull();
                assertThatJson(response.body().string())
                        .isArray()
                        .first().node("guestName").isEqualTo("Alice Smith");
            }
        });
    }

    @Test
    void testUpdateBooking() {
        JavalinTest.test(app, (server, client) -> {
            var updatedMap = Map.of(
                    "hotelName", "HotelB",
                    "guestName", "GuestB",
                    "checkIn", "2024-07-10",
                    "checkOut", "2024-07-15"
            );
            Booking updatedBooking;
            try (var response = client.put("/bookings/" + bookings.getFirst().id(), updatedMap)) {
                updatedBooking = bookings.getFirst();
                assertThat(response.code()).isEqualTo(204);
            }
            assertThat(updatedBooking.hotelName()).isEqualTo("HotelB");
            assertThat(updatedBooking.guestName()).isEqualTo("GuestB");
            assertThat(updatedBooking.checkIn()).isEqualTo(LocalDate.of(2024, 7, 10));
            assertThat(updatedBooking.checkOut()).isEqualTo(LocalDate.of(2024, 7, 15));
        });
    }

    @Test
    void testPatchBooking() {
        JavalinTest.test(app, (server, client) -> {
            var patch = Map.of("guestName", "PatchedName");
            try (var response = client.patch("/bookings/" + bookings.getFirst().id(), patch)) {
                assertThat(response.code()).isEqualTo(204);
            }

            var patchedBooking = bookings.getFirst();
            assertThat(patchedBooking.guestName()).isEqualTo("PatchedName");
            assertThat(patchedBooking.hotelName()).isEqualTo("Hotel California"); // unchanged
            assertThat(patchedBooking.checkIn()).isEqualTo(LocalDate.of(2024, 7, 1)); // unchanged
            assertThat(patchedBooking.checkOut()).isEqualTo(LocalDate.of(2024, 7, 5)); // unchanged

        });
    }

    @Test
    void testDeleteBooking() {
        JavalinTest.test(app, (server, client) -> {
            var deleteUrl = "/bookings/" + bookings.getFirst().id();

            try (var response = client.delete(deleteUrl)) {
                assertThat(response.code()).isEqualTo(204);
            }

            try (var response2 = client.delete(deleteUrl)) {
                assertThat(response2.code()).isEqualTo(404);
            }

            assertThat(bookings).hasSize(2);
        });
    }

    @Test
    void testPipelinrPipelineWithHandlers() {
        UUID id = pipelinr.send(new BookHotelCommand("HotelX", "Eve", LocalDate.now(), LocalDate.now().plusDays(2)));
        assertThat(id).isNotNull();

        pipelinr.send(new GetBookingsQuery(Optional.empty(), Optional.empty()));
        assertThat(bookings).hasSize(4);

        boolean updated = pipelinr.send(new UpdateBookingCommand(id, "UpdatedHotel", "Eve", LocalDate.now(), LocalDate.now().plusDays(3)));
        assertThat(updated).isTrue();

        boolean patched = pipelinr.send(new PatchBookingCommand(id, Map.of("guestName", "Frank")));
        assertThat(patched).isTrue();

        boolean deleted = pipelinr.send(new DeleteBookingCommand(id));
        assertThat(deleted).isTrue();
        assertThat(bookings).hasSize(3);
    }
}
