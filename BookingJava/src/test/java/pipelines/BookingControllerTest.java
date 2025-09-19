package pipelines;

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
import pipelines.controllers.BookingController;
import pipelines.data.InMemoryBookingRepository;
import pipelines.domain.Booking;
import pipelines.infrastructure.BookingWebSocketHub;
import pipelines.infrastructure.EmailService;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static pipelines.infrastructure.WebAppCreator.createJavalinApp;

class BookingControllerTest {
    private static final UUID notExistingId = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
    private LinkedHashMap<@NotNull UUID, @NotNull Booking> bookings;
    private Javalin app;

    @BeforeEach
    void setup() {
        bookings = TestingInfrastructure.getExampleBookings();

        var repository = new InMemoryBookingRepository(bookings) {
            private final AtomicLong counter = new AtomicLong(10L);

            @Override
            public UUID getNextId() {
                return new UUID(0L, counter.getAndIncrement());
            }
        };

        var bookingController = new BookingController(TestingInfrastructure.createPipeline(repository, mock(EmailService.class)), mock(BookingWebSocketHub.class));

        app = createJavalinApp(bookingController, config -> config.showJavalinBanner = false);
    }

    private Booking getFirstBooking() {
        return bookings.values().iterator().next();
    }

    private Booking getLastBooking() {
        return bookings.values().stream().reduce((first, second) -> second).orElse(null);
    }

    @Test
    void testCreateBooking() {
        JavalinTest.test(app, (server, client) -> {
            var booking = Map.of(
                    "hotelName", "TestHotel",
                    "guestName", "John Doe",
                    "email", "email@gmail.com",
                    "checkIn", "2024-07-01",
                    "checkOut", "2024-07-05"
            );
            try (var postRes = client.post("/bookings", booking)) {
                assertThat(postRes.code()).isEqualTo(201);
                assertThat(postRes.body()).isNotNull();
                assertThatJson(postRes.body().string()).node("bookingId").isEqualTo(getLastBooking().id().toString());
            }
        });
    }

    @Test
    void testGetBooking() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.get("/bookings?filter=hotelName has 'Budapest' and guestName has 'Bob'&sort=guestName DESC")) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body()).isNotNull();
                assertThatJson(response.body().string())
                        .isArray()
                        .first().node("guestName").isEqualTo("Bob Johnson");
            }
        });
    }

    @Test
    void testGetByIdBooking() {
        JavalinTest.test(app, (server, client) -> {
            try (var response = client.get("/bookings/" + getFirstBooking().id())) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body()).isNotNull();
                assertThatJson(response.body().string())
                        .node("guestName").isEqualTo(getFirstBooking().guestName());
            }
        });
    }

    @Test
    void testUpdateBooking() {
        JavalinTest.test(app, (server, client) -> {
            var updatedMap = Map.of(
                    "hotelName", "HotelB",
                    "guestName", "GuestB",
                    "email", "new@gmail.com",
                    "checkIn", "2024-07-10",
                    "checkOut", "2024-07-15"
            );
            Booking updatedBooking;
            try (var response = client.put("/bookings/" + getFirstBooking().id(), updatedMap)) {
                updatedBooking = getFirstBooking();
                assertThat(response.code()).isEqualTo(204);
            }
            assertThat(updatedBooking.hotelName()).isEqualTo("HotelB");
            assertThat(updatedBooking.guestName()).isEqualTo("GuestB");
            assertThat(updatedBooking.email()).isEqualTo("new@gmail.com");
            assertThat(updatedBooking.checkIn()).isEqualTo(LocalDate.of(2024, 7, 10));
            assertThat(updatedBooking.checkOut()).isEqualTo(LocalDate.of(2024, 7, 15));
        });
    }

    @Test
    void testPatchBooking() {
        JavalinTest.test(app, (server, client) -> {
            var patch = Map.of("guestName", "PatchedName");
            try (var response = client.patch("/bookings/" + getFirstBooking().id(), patch)) {
                assertThat(response.code()).isEqualTo(204);
            }

            var patchedBooking = getFirstBooking();
            assertThat(patchedBooking.guestName()).isEqualTo("PatchedName");
            assertThat(patchedBooking.hotelName()).isEqualTo("Hotel California"); // unchanged
            assertThat(patchedBooking.email()).isEqualTo("alice.beauty@buziaczek.pl"); // unchanged
            assertThat(patchedBooking.checkIn()).isEqualTo(LocalDate.of(2024, 7, 1)); // unchanged
            assertThat(patchedBooking.checkOut()).isEqualTo(LocalDate.of(2024, 7, 5)); // unchanged

        });
    }

    @Test
    void testDeleteBooking() {
        JavalinTest.test(app, (server, client) -> {
            var deleteUrl = "/bookings/" + getFirstBooking().id();

            try (var response = client.delete(deleteUrl)) {
                assertThat(response.code()).isEqualTo(204);
            }

            try (var response2 = client.delete(deleteUrl)) {
                assertThat(response2.code()).isEqualTo(404);
            }

            assertThat(bookings).hasSize(2);
        });
    }

    static Stream<Arguments> invalidCreateRequests() {
        // base valid request
        Map<String, String> validRequest = Map.of(
                "hotelName", "TestHotel",
                "guestName", "John",
                "email", "email@gmail.com",
                "checkIn", "2024-07-01",
                "checkOut", "2024-07-05"
        );

        return Stream.of(
                // Missing fields
                Arguments.of(modify(validRequest, m -> m.remove("hotelName")),
                        "Missing or empty required field: hotelName"),

                Arguments.of(modify(validRequest, m -> m.remove("guestName")),
                        "Missing or empty required field: guestName"),

                Arguments.of(modify(validRequest, m -> m.remove("email")),
                        "Missing or empty required field: email"),

                Arguments.of(modify(validRequest, m -> m.remove("checkIn")),
                        "Missing required field: checkIn"),

                Arguments.of(modify(validRequest, m -> m.remove("checkOut")),
                        "Missing required field: checkOut"),

                // Invalid date formats
                Arguments.of(modify(validRequest, m -> m.put("checkIn", "invalid")),
                        "Expected ISO-8601 (yyyy-MM-dd) format for field checkIn"),

                Arguments.of(modify(validRequest, m -> m.put("checkOut", "2024-13-01")),
                        "Expected ISO-8601 (yyyy-MM-dd) format for field checkOut"),

                // Invalid email format
                Arguments.of(modify(validRequest, m -> m.put("email", "emailWithoutAtChar")),
                        "A valid email is required"),

                // Business rule
                Arguments.of(modify(validRequest, m -> {
                            m.put("checkIn", "2024-07-10");
                            m.put("checkOut", "2024-07-01");
                        }),
                        "checkIn cannot be after checkOut")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidCreateRequests")
    @DisplayName("❌ Negative validation for creating bookings")
    void testCreateBookingValidation(Map<String, Object> booking, String expectedError) {
        JavalinTest.test(app, (server, client) -> {
            try (var res = client.post("/bookings", booking)) {
                assertThat(res.code()).isEqualTo(400);

                assertThat(res.body()).isNotNull();
                assertThat(res.body().string()).contains(expectedError);
            }
        });
    }

    static Stream<Arguments> deleteBookingCases() {
        return Stream.of(
                Arguments.of(Named.of("Invalid UUID format", "not-a-uuid"), 400, "Invalid format for UUID path parameter id"),

                Arguments.of(Named.of("Non-existing booking", notExistingId.toString()), 404, null)
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
                    assertThat(res.body()).isNotNull();
                    assertThat(res.body().string()).contains(expectedMessage);
                }
            }
        });
    }

    static Stream<Arguments> invalidUpdateRequests() {
        var validId = new UUID(0L, 1L).toString();

        Map<String, String> validUpdateRequest = Map.of(
                "hotelName", "TestHotel",
                "guestName", "John",
                "email", "email@gmail.com",
                "checkIn", "2024-07-01",
                "checkOut", "2024-07-05"
        );

        return Stream.of(
                // Missing fields
                Arguments.of(modify(validUpdateRequest, m -> m.remove("hotelName")),
                        validId, "Missing or empty required field: hotelName", 400),

                Arguments.of(modify(validUpdateRequest, m -> m.remove("guestName")),
                        validId, "Missing or empty required field: guestName", 400),

                Arguments.of(modify(validUpdateRequest, m -> m.remove("email")),
                        validId, "Missing or empty required field: email", 400),

                Arguments.of(modify(validUpdateRequest, m -> m.remove("checkIn")),
                        validId, "Missing required field: checkIn", 400),

                Arguments.of(modify(validUpdateRequest, m -> m.remove("checkOut")),
                        validId, "Missing required field: checkOut", 400),

                // Invalid date formats
                Arguments.of(modify(validUpdateRequest, m -> m.put("checkIn", "invalid")),
                        validId, "Expected ISO-8601 (yyyy-MM-dd) format for field checkIn", 400),

                Arguments.of(modify(validUpdateRequest, m -> m.put("checkOut", "2024-13-01")),
                        validId, "Expected ISO-8601 (yyyy-MM-dd) format for field checkOut", 400),

                // Business rule
                Arguments.of(modify(validUpdateRequest, m -> {
                            m.put("checkIn", "2024-07-10");
                            m.put("checkOut", "2024-07-01");
                        }),
                        validId, "checkIn cannot be after checkOut", 400),

                // Invalid UUID path parameter
                Arguments.of(validUpdateRequest, "invalid-id", "Invalid UUID", 400),

                // Not existing ID
                Arguments.of(validUpdateRequest, notExistingId.toString(), "Booking ID not found", 404)
        );
    }

    @ParameterizedTest(name = "{3}: {2}")
    @MethodSource("invalidUpdateRequests")
    void testUpdateBookingValidation(Map<String, Object> requestBody, String bookingId, String expectedError, int expectedStatus) {
        JavalinTest.test(app, (server, client) -> {
            try (var res = client.put("/bookings/" + bookingId, requestBody)) {
                assertThat(res.code()).isEqualTo(expectedStatus);
                if (expectedError != null) {
                    assertThat(res.body()).isNotNull();
                    assertThat(res.body().string()).contains(expectedError);
                }
            }
        });
    }

    private static Map<String, String> modify(Map<String, String> base, Consumer<Map<String, String>> mutator) {
        Map<String, String> copy = new HashMap<>(base);
        mutator.accept(copy);
        return copy;
    }
}
