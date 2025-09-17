package pipelines;

import an.awesome.pipelinr.Pipeline;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
record BookingController(Pipeline pipeline) {
    public void registerRoutes(Javalin app) {
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()));
        });

        // POST /bookings
        app.post("/bookings", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = requireNonEmptyString(body, "hotelName");
            var guest = requireNonEmptyString(body, "guestName");
            var checkIn = getRequiredDate(body, "checkIn");
            var checkOut = getRequiredDate(body, "checkOut");


            var bookingId = pipeline.send(new BookHotelCommand(hotel, guest, checkIn, checkOut));
            ctx.status(HttpStatus.CREATED)
                    .header("Location", "/bookings/" + bookingId)
                    .json(Map.of("bookingId", bookingId.toString()));
        });

        // DELETE /bookings/{bookingId}
        app.delete("/bookings/{bookingId}", ctx -> {
            UUID bookingId = getUuidFromPath(ctx, "bookingId");

            boolean deleted = pipeline.send(new DeleteBookingCommand(bookingId));
            ctx.status(deleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND);
        });

        // GET /bookings/{bookingId}
        app.get("/bookings/{bookingId}", ctx -> {
            UUID bookingId = getUuidFromPath(ctx, "bookingId");
            var bookings = pipeline.send(new GetBookingsByIdQuery(bookingId));
            if (bookings != null)
                ctx.json(bookings).status(HttpStatus.OK);
            else ctx.status(HttpStatus.NOT_FOUND);
        });

        // GET /bookings?hotelName=...&guestName=...
        app.get("/bookings", ctx -> {
            var hotelName = Optional.ofNullable(ctx.queryParam("hotelName"));
            var guestName = Optional.ofNullable(ctx.queryParam("guestName"));

            var bookings = pipeline.send(new GetBookingsQuery(hotelName, guestName));
            ctx.json(bookings).status(HttpStatus.OK);
        });

        // PUT /bookings/{bookingId}
        app.put("/bookings/{bookingId}", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = requireNonEmptyString(body, "hotelName");
            var guest = requireNonEmptyString(body, "guestName");
            var checkIn = getRequiredDate(body, "checkIn");
            var checkOut = getRequiredDate(body, "checkOut");


            var bookingId = getUuidFromPath(ctx, "bookingId");
            boolean updated = pipeline.send(new UpdateBookingCommand(bookingId, hotel, guest, checkIn, checkOut));

            if (updated) ctx.status(HttpStatus.NO_CONTENT);
            else ctx.status(HttpStatus.NOT_FOUND).result("Booking ID not found: " + bookingId);
        });

        // PATCH /bookings/{bookingId}
        app.patch("/bookings/{bookingId}", ctx -> {
            var bookingId = getUuidFromPath(ctx, "bookingId");
            var body = ctx.bodyAsClass(Map.class);

            boolean patched = pipeline.send(new PatchBookingCommand(bookingId, body));
            if (patched) ctx.status(HttpStatus.NO_CONTENT);
            else ctx.status(HttpStatus.NOT_FOUND).result("Booking ID not found: " + bookingId);
        });
    }

    private static UUID getUuidFromPath(Context ctx, String fieldName) {
        try {
            return UUID.fromString(ctx.pathParam(fieldName));
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid format for UUID path parameter %s: %s".formatted(fieldName, e.getMessage()));
        }
    }

    private static String requireNonEmptyString(Map<?, ?> body, String fieldName) {
        var value = body.get(fieldName);
        if (value == null || ((String) value).isBlank()) throw new BadRequestResponse("Missing or empty required field: " + fieldName);
        return (String) value;
    }

    private static LocalDate getRequiredDate(Map<?, ?> body, String fieldName) {
        var value = body.get(fieldName);
        if (value == null) throw new BadRequestResponse("Missing required field: " + fieldName);
        try {
            return LocalDate.parse((String) value);
        } catch (Exception e) {
            throw new BadRequestResponse("Expected ISO-8601 (yyyy-MM-dd) format for field %s: %s".formatted(fieldName, e.getMessage()));
        }
    }
}
