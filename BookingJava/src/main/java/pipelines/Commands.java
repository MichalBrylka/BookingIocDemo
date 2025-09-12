package pipelines;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.*;

record BookHotelCommand(String hotelName, String guestName, LocalDate checkIn, LocalDate checkOut) implements Command<UUID> {}


record GetBookingsQuery(Optional<String> hotelName, Optional<String> guestName) implements Command<List<Booking>> {
    public GetBookingsQuery() {
        this(Optional.empty(), Optional.empty());
    }
}
