package pipelines.controllers;

import an.awesome.pipelinr.Pipeline;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.openapi.*;
import org.springframework.stereotype.Component;
import pipelines.commands.*;
import pipelines.data.DataExpressionParser;
import pipelines.domain.Booking;
import pipelines.infrastructure.BookingWebSocketHub;
import pipelines.response.*;

import java.time.LocalDate;
import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.*;

@Component
public record BookingController(Pipeline pipeline, BookingWebSocketHub webSocketHub) implements EndpointGroup {
    private static final String RESOURCE_NAME = "Bookings";
    private static final String BASE_PATH = "/bookings";
    private static final String ID_PATH = BASE_PATH + "/{id}";

    @Override
    public void addEndpoints() {
        path(RESOURCE_NAME.toLowerCase(), () -> {
            post(this::createBooking);
            get(this::listBookings);

            path("{id}", () -> {
                get(this::getBooking);
                put(this::updateBooking);
                patch(this::patchBooking);
                delete(this::deleteBooking);
            });

            ws("events", ws -> {
                ws.onConnect(ctx -> {
                    webSocketHub.register(ctx);
                    System.out.println("Client connected: " + ctx.sessionId());
                });
                ws.onClose(ctx -> {
                    webSocketHub.unregister(ctx);
                    System.out.println("Client disconnected: " + ctx.sessionId());
                });
            });
        });
    }


    @OpenApi(summary = "Create a new booking",
            description = "Creates a booking and broadcasts an event to all connected WebSocket clients",
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = BookHotelCommand.class), required = true),
            tags = {RESOURCE_NAME},
            path = BASE_PATH,
            methods = {HttpMethod.POST},
            responses = {
                    @OpenApiResponse(status = "201", content = @OpenApiContent(from = BookingCreatedResponse.class)),
                    @OpenApiResponse(status = "400", description = "Missing or invalid fields, dates not in ISO-8601 format, checkOut before checkIn")
            }
    )
    private void createBooking(Context ctx) {
        var body = ctx.bodyAsClass(Map.class);
        var hotel = requireNonEmptyString(body, "hotelName");
        var guest = requireNonEmptyString(body, "guestName");
        var email = requireNonEmptyString(body, "email");
        var checkIn = getRequiredDate(body, "checkIn");
        var checkOut = getRequiredDate(body, "checkOut");

        var bookingId = pipeline.send(new BookHotelCommand(hotel, guest, email, checkIn, checkOut));

        webSocketHub.broadcast(Map.of("event", "BookingCreated", "bookingId", bookingId.toString(), "guestName", guest, "hotelName", hotel));


        String baseUrl = ctx.url();
        String bookingUrl = baseUrl + "/" + bookingId;


        ctx.status(HttpStatus.CREATED)
                .header("Location", bookingUrl)
                .json(
                        new BookingCreatedResponse(
                                bookingId.toString(),
                                new Links(
                                        new Link(bookingUrl, "GET"),
                                        new Link(bookingUrl, "PUT"),
                                        new Link(bookingUrl, "PATCH"),
                                        new Link(bookingUrl, "DELETE"),
                                        new Link(baseUrl, "GET")
                                )
                        )
                );
    }

    @OpenApi(summary = "Get all bookings. Supported fields: hotelName, guestName, email, checkIn, checkOut.",
            operationId = "getAllBookings",
            responses = {@OpenApiResponse(status = "200", content = {@OpenApiContent(from = Booking[].class)})},
            tags = {RESOURCE_NAME},
            path = BASE_PATH,
            methods = {HttpMethod.GET},
            queryParams = {
                    @OpenApiParam(name = "filter",
                            description = "Filter bookings by criteria. Supported operators: eq, neq, has (for strings), dates operators (gt, lt, gte, lte). Combine conditions with AND",
                            example = "guestName eq 'John' and hotelName eq 'Hilton'"
                    ),
                    @OpenApiParam(name = "sort",
                            description = "Sort bookings by field name and direction (ASC-default or DESC). Multiple fields can be separated by commas.",
                            example = "checkIn DESC, hotelName ASC, guestName"
                    )
            }
    )
    private void listBookings(Context ctx) {
        var filter = DataExpressionParser.parseFilter(ctx.queryParam("filter"), Booking.class);
        var sort = DataExpressionParser.parseSort(ctx.queryParam("sort"));

        var bookings = pipeline.send(new GetBookingsQuery(filter, sort));
        ctx.json(bookings).status(HttpStatus.OK);
    }

    private void getBooking(Context ctx) {
        UUID bookingId = getUuidFromPath(ctx);
        var bookings = pipeline.send(new GetBookingsByIdQuery(bookingId));
        if (bookings != null)
            ctx.json(bookings).status(HttpStatus.OK);
        else ctx.status(HttpStatus.NOT_FOUND);
    }

    private void updateBooking(Context ctx) {
        var body = ctx.bodyAsClass(Map.class);
        var hotel = requireNonEmptyString(body, "hotelName");
        var guest = requireNonEmptyString(body, "guestName");
        var email = requireNonEmptyString(body, "email");
        var checkIn = getRequiredDate(body, "checkIn");
        var checkOut = getRequiredDate(body, "checkOut");


        var bookingId = getUuidFromPath(ctx);
        boolean updated = pipeline.send(new UpdateBookingCommand(bookingId, hotel, guest, email, checkIn, checkOut));

        if (updated)
            webSocketHub.broadcast(Map.of(
                    "event", "BookingUpdated",
                    "bookingId", bookingId.toString()
            ));

        if (updated) ctx.status(HttpStatus.NO_CONTENT);
        else ctx.status(HttpStatus.NOT_FOUND).result("Booking ID not found: " + bookingId);
    }

    private void patchBooking(Context ctx) {
        var bookingId = getUuidFromPath(ctx);
        var body = ctx.bodyAsClass(Map.class);

        boolean patched = pipeline.send(new PatchBookingCommand(bookingId, body));

        if (patched)
            webSocketHub.broadcast(Map.of(
                    "event", "BookingPatched",
                    "bookingId", bookingId.toString()
            ));

        if (patched) ctx.status(HttpStatus.NO_CONTENT);
        else ctx.status(HttpStatus.NOT_FOUND).result("Booking ID not found: " + bookingId);
    }

    private void deleteBooking(Context ctx) {
        UUID bookingId = getUuidFromPath(ctx);

        boolean deleted = pipeline.send(new DeleteBookingCommand(bookingId));
        if (deleted)
            webSocketHub.broadcast(Map.of(
                    "event", "BookingDeleted",
                    "bookingId", bookingId.toString()
            ));
        ctx.status(deleted ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND);
    }

    private static UUID getUuidFromPath(Context ctx) {
        try {
            return UUID.fromString(ctx.pathParam("id"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid format for UUID path parameter %s: %s".formatted("id", e.getMessage()));
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