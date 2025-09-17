package pipelines;

import an.awesome.pipelinr.Pipeline;
import io.javalin.Javalin;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
record BookingController(Pipeline pipeline) {

    public void registerRoutes(Javalin app) {
        // POST /bookings
        app.post("/bookings", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = (String) body.get("hotelName");
            var guest = (String) body.get("guestName");
            var checkIn = LocalDate.parse((String) body.get("checkIn"));
            var checkOut = LocalDate.parse((String) body.get("checkOut"));

            var bookingId = pipeline.send(
                    new BookHotelCommand(hotel, guest, checkIn, checkOut)
            );
            ctx.json(Map.of("bookingId", bookingId.toString()));
        });

        // DELETE /bookings/{bookingId}
        app.delete("/bookings/{bookingId}", ctx -> {

            String bookingIdStr = ctx.pathParam("bookingId");
            UUID bookingId = null;
            try {
                bookingId = UUID.fromString(bookingIdStr);
            } catch (IllegalArgumentException e) {
                ctx.status(400).result("Invalid bookingId format: " + e.getMessage());
            }
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
            String bookingId = ctx.pathParam("bookingId");
            var body = ctx.bodyAsClass(Map.class);
            try {
                var hotel = (String) body.get("hotelName");
                var guest = (String) body.get("guestName");
                var checkIn = LocalDate.parse((String) body.get("checkIn"));
                var checkOut = LocalDate.parse((String) body.get("checkOut"));
                boolean updated = pipeline.send(new UpdateBookingCommand(
                        java.util.UUID.fromString(bookingId), hotel, guest, checkIn, checkOut
                ));
                ctx.status(updated ? 204 : 404);
            } catch (Exception e) {
                ctx.status(400).result("Invalid request: " + e.getMessage());
            }
        });

        // PATCH /bookings/{bookingId}
        app.patch("/bookings/{bookingId}", ctx -> {
            String bookingId = ctx.pathParam("bookingId");
            var body = ctx.bodyAsClass(Map.class);
            try {
                boolean patched = pipeline.send(new PatchBookingCommand(
                        java.util.UUID.fromString(bookingId), body
                ));
                ctx.status(patched ? 204 : 404);
            } catch (Exception e) {
                ctx.status(400).result("Invalid request: " + e.getMessage());
            }
        });
    }
}
