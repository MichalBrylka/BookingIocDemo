package pipelines;

import an.awesome.pipelinr.Pipeline;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.websocket.WsContext;
import org.springframework.stereotype.Component;
import pipelines.BookingFilter.*;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

@Component
class BookingController {
    private final Pipeline pipeline;
    private final BookingWebSocketHub webSocketHub = new BookingWebSocketHub();

    BookingController(Pipeline pipeline) {this.pipeline = pipeline;}

    public void registerRoutes(Javalin app) {
        app.exception(IllegalArgumentException.class, (e, ctx) ->
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", e.getMessage()))
        );

        app.ws("/ws/bookings", ws -> {
            ws.onConnect(ctx -> {
                webSocketHub.register(ctx);
                System.out.println("Client connected: " + ctx.sessionId());
            });
            ws.onClose(ctx -> {
                webSocketHub.unregister(ctx);
                System.out.println("Client disconnected: " + ctx.sessionId());
            });
        });

        // POST /bookings
        app.post("/bookings", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = requireNonEmptyString(body, "hotelName");
            var guest = requireNonEmptyString(body, "guestName");
            var email = requireNonEmptyString(body, "email");
            var checkIn = getRequiredDate(body, "checkIn");
            var checkOut = getRequiredDate(body, "checkOut");


            var bookingId = pipeline.send(new BookHotelCommand(hotel, guest, email, checkIn, checkOut));

            webSocketHub.broadcast(Map.of(
                    "event", "BookingCreated",
                    "bookingId", bookingId.toString(),
                    "guestName", guest,
                    "hotelName", hotel
            ));

            ctx.status(HttpStatus.CREATED)
                    .header("Location", "/bookings/" + bookingId)
                    .json(Map.of("bookingId", bookingId.toString()));
        });

        // DELETE /bookings/{bookingId}
        app.delete("/bookings/{bookingId}", ctx -> {
            UUID bookingId = getUuidFromPath(ctx, "bookingId");

            boolean deleted = pipeline.send(new DeleteBookingCommand(bookingId));
            if (deleted)
                webSocketHub.broadcast(Map.of(
                        "event", "BookingDeleted",
                        "bookingId", bookingId.toString()
                ));
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

        // GET /bookings?filter=guestName eq 'John' and hotelName eq 'Hilton'
        app.get("/bookings", ctx -> {
            var filter = BookingExpressionParser.parseFilter(ctx.queryParam("filter"));
            var sort = BookingExpressionParser.parseSort(ctx.queryParam("sort"));

            var bookings = pipeline.send(new GetBookingsQuery(filter, sort));
            ctx.json(bookings).status(HttpStatus.OK);
        });

        // PUT /bookings/{bookingId}
        app.put("/bookings/{bookingId}", ctx -> {
            var body = ctx.bodyAsClass(Map.class);
            var hotel = requireNonEmptyString(body, "hotelName");
            var guest = requireNonEmptyString(body, "guestName");
            var email = requireNonEmptyString(body, "email");
            var checkIn = getRequiredDate(body, "checkIn");
            var checkOut = getRequiredDate(body, "checkOut");


            var bookingId = getUuidFromPath(ctx, "bookingId");
            boolean updated = pipeline.send(new UpdateBookingCommand(bookingId, hotel, guest, email, checkIn, checkOut));

            if (updated)
                webSocketHub.broadcast(Map.of(
                        "event", "BookingUpdated",
                        "bookingId", bookingId.toString()
                ));

            if (updated) ctx.status(HttpStatus.NO_CONTENT);
            else ctx.status(HttpStatus.NOT_FOUND).result("Booking ID not found: " + bookingId);
        });

        // PATCH /bookings/{bookingId}
        app.patch("/bookings/{bookingId}", ctx -> {
            var bookingId = getUuidFromPath(ctx, "bookingId");
            var body = ctx.bodyAsClass(Map.class);

            boolean patched = pipeline.send(new PatchBookingCommand(bookingId, body));

            if (patched)
                webSocketHub.broadcast(Map.of(
                        "event", "BookingPatched",
                        "bookingId", bookingId.toString()
                ));

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

class BookingWebSocketHub {
    private final Set<WsContext> sessions = new java.util.concurrent.CopyOnWriteArraySet<>();

    public void register(WsContext ctx) {
        sessions.add(ctx);
    }

    public void unregister(WsContext ctx) {
        sessions.remove(ctx);
    }

    public void broadcast(Object message) {
        for (WsContext session : sessions) {
            if (session.session.isOpen()) {
                session.send(message);
            }
        }
    }
}

class BookingExpressionParser {

    public static Iterable<SortField> parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) return null;

        List<SortField> sortFields = new ArrayList<>();
        for (String part : sortParam.split(",")) {
            String[] tokens = part.trim().split("\\s+");
            String field = tokens[0];
            boolean descending = tokens.length == 2 && tokens[1].equalsIgnoreCase("DESC");
            sortFields.add(new SortField(field, !descending));
        }
        return sortFields;
    }

    // regex: field operator 'value', value can have '' escaped quote
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
            "(\\w+)\\s+(eq|neq|has|gt|lt|gte|lte)\\s+'((?:[^']|'')*)'",
            Pattern.CASE_INSENSITIVE
    );

    public static BookingFilter parseFilter(String filterParam) {
        if (filterParam == null || filterParam.isBlank()) return null;

        StringFilter hotelName = null;
        StringFilter guestName = null;
        StringFilter email = null;
        DateFilter checkIn = null;
        DateFilter checkOut = null;

        // Split by AND (case-insensitive)
        String[] conditions = filterParam.split("(?i)\\s+AND\\s+");
        for (String condition : conditions) {
            Matcher matcher = CONDITION_PATTERN.matcher(condition.trim());
            if (!matcher.matches()) continue; // skip invalid

            String field = matcher.group(1);
            String op = matcher.group(2);
            String valueRaw = matcher.group(3).replace("''", "'"); // unescape ''

            switch (field) {
                case "hotelName" -> hotelName = new StringFilter(valueRaw, parseStringOperator(op));
                case "guestName" -> guestName = new StringFilter(valueRaw, parseStringOperator(op));
                case "email" -> email = new StringFilter(valueRaw, parseStringOperator(op));
                case "checkIn" -> checkIn = new DateFilter(LocalDate.parse(valueRaw), parseDateOperator(op));
                case "checkOut" -> checkOut = new DateFilter(LocalDate.parse(valueRaw), parseDateOperator(op));
                default -> throw new IllegalArgumentException("Unknown field: " + field);
            }
        }

        return new BookingFilter(hotelName, guestName, email, checkIn, checkOut);
    }

    private static BookingFilter.Operator parseStringOperator(String op) {
        return switch (op.toLowerCase()) {
            case "eq" -> BookingFilter.Operator.EQ;
            case "neq" -> BookingFilter.Operator.NEQ;
            case "has" -> BookingFilter.Operator.IN;
            default -> throw new IllegalArgumentException("Unknown string operator: " + op);
        };
    }

    private static BookingFilter.Operator parseDateOperator(String op) {
        return switch (op.toLowerCase()) {
            case "eq" -> BookingFilter.Operator.EQ;
            case "neq" -> BookingFilter.Operator.NEQ;
            case "gt" -> BookingFilter.Operator.GT;
            case "lt" -> BookingFilter.Operator.LT;
            case "gte" -> BookingFilter.Operator.GTE;
            case "lte" -> BookingFilter.Operator.LTE;
            default -> throw new IllegalArgumentException("Unknown date operator: " + op);
        };
    }
}