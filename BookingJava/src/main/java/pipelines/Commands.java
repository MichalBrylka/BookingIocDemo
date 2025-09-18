package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;

import java.time.LocalDate;
import java.util.*;

interface HasBookingDates {
    LocalDate checkIn();

    LocalDate checkOut();
}

record BookHotelCommand(String hotelName, String guestName, String email, LocalDate checkIn, LocalDate checkOut) implements Command<UUID>, HasBookingDates {}

record GetBookingsByIdQuery(UUID bookingId) implements Command<Booking> {}

record GetBookingsQuery(Optional<String> hotelName, Optional<String> guestName) implements Command<List<Booking>> {
    public GetBookingsQuery() {
        this(Optional.empty(), Optional.empty());
    }
}

record DeleteBookingCommand(UUID bookingId) implements Command<Boolean> {}

record UpdateBookingCommand(UUID bookingId, String hotelName, String guestName, String email, LocalDate checkIn,
                            LocalDate checkOut) implements Command<Boolean>, HasBookingDates {}

record PatchBookingCommand(UUID bookingId, Map<String, Object> fields) implements Command<Boolean> {}


record BookingConfirmedNotification(Booking booking) implements Notification {}
