package pipelines;

import an.awesome.pipelinr.Pipeline;
import io.javalin.Javalin;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

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
            String bookingId = ctx.pathParam("bookingId");
            try {
                boolean entityFound = pipeline.send(new DeleteBookingCommand(java.util.UUID.fromString(bookingId)));
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
    }
}
