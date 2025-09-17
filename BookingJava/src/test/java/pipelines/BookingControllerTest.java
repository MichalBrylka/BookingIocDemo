package pipelines;

import an.awesome.pipelinr.Pipelinr;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                // Missing fields
                Arguments.of(Named.of("Missing hotelName",
                                Map.of("guestName", "John", "checkIn", "2024-07-01", "checkOut", "2024-07-05")),
                        "Missing required field: hotelName"),

                Arguments.of(Named.of("Missing guestName",
                                Map.of("hotelName", "TestHotel", "checkIn", "2024-07-01", "checkOut", "2024-07-05")),
                        "Missing required field: guestName"),

                Arguments.of(Named.of("Missing checkIn",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkOut", "2024-07-05")),
                        "Missing required field: checkIn"),

                Arguments.of(Named.of("Missing checkOut",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-01")),
                        "Missing required field: checkOut"),

                // Invalid date formats
                Arguments.of(Named.of("Invalid checkIn format",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "invalid", "checkOut", "2024-07-05")),
                        "Expected ISO-8601 (yyyy-MM-dd) format for field checkIn"),

                Arguments.of(Named.of("Invalid checkOut format",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-01", "checkOut", "2024-13-01")),
                        "Expected ISO-8601 (yyyy-MM-dd) format for field checkOut"),

                // Business rule
                Arguments.of(Named.of("checkIn after checkOut",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-10", "checkOut", "2024-07-01")),
                        "checkIn cannot be after checkOut")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    @DisplayName("❌ Negative validation for creating bookings")
    void testCreateBookingValidation(Map<String, Object> booking, String expectedError) {
        JavalinTest.test(app, (server, client) -> {
            try (var res = client.post("/bookings", booking)) {
                assertThat(res.code()).isEqualTo(400);
                var body = res.body().string();
                assertThat(body).contains(expectedError);
            }
        });
    }

    static Stream<Arguments> deleteBookingCases() {
        return Stream.of(
                Arguments.of(Named.of("Invalid UUID format", "not-a-uuid"), 400, "Invalid format for UUID path parameter bookingId: Invalid UUID string: not-a-uuid"),

                Arguments.of(Named.of("Non-existing booking", new UUID(Long.MAX_VALUE, Long.MAX_VALUE).toString()), 404, null)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("deleteBookingCases")
    @DisplayName("❌ Negative validation for deleting bookings")
    void testDeleteBookingValidation(String bookingIdStr, int expectedStatus, String expectedMessage) {
        JavalinTest.test(app, (server, client) -> {
            try (var res = client.delete("/bookings/" + bookingIdStr)) {
                assertThat(res.code()).isEqualTo(expectedStatus);
                if (expectedMessage != null) {
                    var body = res.body().string();
                    assertThat(body).contains(expectedMessage);
                }
            }
        });
    }

    static Stream<Arguments> invalidUpdateRequests() {
        var validId = new UUID(0L, 1L).toString();
        return Stream.of(
                // Missing fields
                Arguments.of(Named.of("Missing hotelName",
                                Map.of("guestName", "John", "checkIn", "2024-07-01", "checkOut", "2024-07-05")),
                        validId, "hotelName", 400),

                Arguments.of(Named.of("Missing guestName",
                                Map.of("hotelName", "TestHotel", "checkIn", "2024-07-01", "checkOut", "2024-07-05")),
                        validId, "guestName", 400),

                Arguments.of(Named.of("Missing checkIn",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkOut", "2024-07-05")),
                        validId, "checkIn", 400),

                Arguments.of(Named.of("Missing checkOut",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-01")),
                        validId, "checkOut", 400),

                // Invalid date formats
                Arguments.of(Named.of("Invalid checkIn format",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "invalid", "checkOut", "2024-07-05")),
                        validId, "checkIn", 400),

                Arguments.of(Named.of("Invalid checkOut format",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-01", "checkOut", "2024-13-01")),
                        validId, "checkOut", 400),

                // checkIn after checkOut
                Arguments.of(Named.of("checkIn after checkOut",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-10", "checkOut", "2024-07-01")),
                        validId, "checkIn cannot be after checkOut", 400),

                // Invalid UUID path parameter
                Arguments.of(Named.of("Invalid bookingId UUID",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-01", "checkOut", "2024-07-05")),
                        "invalid-id", "Invalid UUID", 400),

                // not existing ID
                Arguments.of(Named.of("Not existing booking UUID",
                                Map.of("hotelName", "TestHotel", "guestName", "John", "checkIn", "2024-07-01", "checkOut", "2024-07-05")),
                        new UUID(Long.MAX_VALUE, Long.MAX_VALUE).toString(), "Booking ID not found", 404)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUpdateRequests")
    void testUpdateBookingValidation(Map<String, Object> requestBody, String bookingId, String expectedError, int expectedStatus) {
        JavalinTest.test(app, (server, client) -> {
            try (var res = client.put("/bookings/" + bookingId, requestBody)) {
                assertThat(res.code()).isEqualTo(expectedStatus);
                if (expectedError != null) {
                    var body = res.body().string();
                    assertThat(body).contains(expectedError);
                }
            }
        });
    }
}
