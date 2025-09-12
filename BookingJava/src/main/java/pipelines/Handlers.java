package pipelines;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
record BookHotelHandler(BookingRepository repository) implements Command.Handler<BookHotelCommand, UUID> {
    @Override
    public UUID handle(BookHotelCommand command) {
        var booking = new Booking(UUID.randomUUID(), command.hotelName(), command.guestName(), command.checkIn(), command.checkOut());
        repository.add(booking);
        return booking.id();
    }
}

@Component
record GetBookingsHandler(BookingRepository repository) implements Command.Handler<GetBookingsQuery, List<Booking>> {
    @Override
    public List<Booking> handle(GetBookingsQuery query) {
        return repository.getAll().stream().filter(b -> query.hotelName().map(name -> b.hotelName().equalsIgnoreCase(name)).orElse(true)).filter(b -> query.guestName().map(guest -> b.guestName().equalsIgnoreCase(guest)).orElse(true)).collect(Collectors.toList());
    }
}

@Component
record DeleteBookingsHandler(BookingRepository repository) implements Command.Handler<DeleteBookingCommand, Boolean> {

    @Override
    public Boolean handle(DeleteBookingCommand deleteBookingCommand) {
        return repository.delete(deleteBookingCommand.bookingId());
    }
}
