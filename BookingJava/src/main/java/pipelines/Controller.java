package pipelines;

import an.awesome.pipelinr.Pipeline;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
record BookingController(Pipeline pipeline) {
    public void registerRoutes(Javalin app) {
        // POST /bookings
        app.post("/bookings", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = getStringFromBody(body, "hotelName");
            var guest = getStringFromBody(body, "guestName");
            var checkIn = getDateFromBody(body, "checkIn");
            var checkOut = getDateFromBody(body, "checkOut");

            if (checkIn.isAfter(checkOut)) throw new BadRequestResponse("checkIn cannot be after checkOut");

            var bookingId = pipeline.send(new BookHotelCommand(hotel, guest, checkIn, checkOut));
            ctx.status(201)
                    .header("Location", "/bookings/" + bookingId)
                    .json(Map.of("bookingId", bookingId.toString()));
        });

        // DELETE /bookings/{bookingId}
        app.delete("/bookings/{bookingId}", ctx -> {
            UUID bookingId = getUuidFromPath(ctx, "bookingId");
            try {
                boolean entityFound = pipeline.send(new DeleteBookingCommand(bookingId));
                ctx.status(entityFound ? 204 : 404);
            } catch (Exception e) {
                ctx.status(500).result("Internal Server Error: " + e.getMessage());
            }
        });


        // GET /bookings?hotelName=...&guestName=...
        app.get("/bookings", ctx -> {
            var hotelName = Optional.ofNullable(ctx.queryParam("hotelName"));
            var guestName = Optional.ofNullable(ctx.queryParam("guestName"));

            try {
                var query = new GetBookingsQuery(hotelName, guestName);
                var bookings = pipeline.send(query);
                ctx.json(bookings);
            } catch (Exception e) {
                ctx.status(500).result("Internal Server Error: " + e.getMessage());
            }
        });

        // PUT /bookings/{bookingId}
        app.put("/bookings/{bookingId}", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = getStringFromBody(body, "hotelName");
            var guest = getStringFromBody(body, "guestName");
            var checkIn = getDateFromBody(body, "checkIn");
            var checkOut = getDateFromBody(body, "checkOut");

            if (checkIn.isAfter(checkOut)) throw new BadRequestResponse("checkIn cannot be after checkOut");

            var bookingId = getUuidFromPath(ctx, "bookingId");

            try {
                boolean updated = pipeline.send(new UpdateBookingCommand(bookingId, hotel, guest, checkIn, checkOut));
                if (updated)
                    ctx.status(204);
                else
                    ctx.status(404).result("Booking ID not found: " + bookingId);
            } catch (Exception e) {
                ctx.status(400).result("Invalid request: " + e.getMessage());
            }
        });

        // PATCH /bookings/{bookingId}
        app.patch("/bookings/{bookingId}", ctx -> {
            var bookingId = getUuidFromPath(ctx, "bookingId");
            var body = ctx.bodyAsClass(Map.class);
            try {
                boolean patched = pipeline.send(new PatchBookingCommand(bookingId, body));
                ctx.status(patched ? 204 : 404);
            } catch (IllegalArgumentException iae) {
                ctx.status(400).result(iae.getMessage());
            } catch (Exception e) {
                ctx.status(400).result("Invalid request: " + e.getMessage());
            }
        });
    }

    private static UUID getUuidFromPath(Context ctx, String fieldName) {
        String text = ctx.pathParam(fieldName);
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid format for UUID path parameter %s: %s".formatted(fieldName, e.getMessage()));
        }
    }

    private static String getStringFromBody(Map<?, ?> body, String fieldName) {
        var param = body.get(fieldName);
        if (param == null) throw new BadRequestResponse("Missing required field: " + fieldName);
        return (String) param;
    }

    private static LocalDate getDateFromBody(Map<?, ?> body, String fieldName) {
        var dateStr = (String) body.get(fieldName);
        if (dateStr == null) throw new BadRequestResponse("Missing required field: " + fieldName);

        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new BadRequestResponse("Expected ISO-8601 (yyyy-MM-dd) format for field %s: %s".formatted(fieldName, e.getMessage()));
        }
    }
}
