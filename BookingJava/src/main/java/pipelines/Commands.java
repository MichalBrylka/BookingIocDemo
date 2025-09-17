package pipelines;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.*;

interface HasBookingDates {
    LocalDate checkIn();

    LocalDate checkOut();
}

record BookHotelCommand(String hotelName, String guestName, LocalDate checkIn, LocalDate checkOut) implements Command<UUID>, HasBookingDates {}


record GetBookingsQuery(Optional<String> hotelName, Optional<String> guestName) implements Command<List<Booking>> {
    public GetBookingsQuery() {
        this(Optional.empty(), Optional.empty());
    }
}

record DeleteBookingCommand(UUID bookingId) implements Command<Boolean> {}

record UpdateBookingCommand(UUID bookingId, String hotelName, String guestName, LocalDate checkIn,
                            LocalDate checkOut) implements Command<Boolean>, HasBookingDates {}

record PatchBookingCommand(UUID bookingId, Map<String, Object> fields) implements Command<Boolean> {}
