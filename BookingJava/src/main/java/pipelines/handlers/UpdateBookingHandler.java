package pipelines.handlers;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import pipelines.commands.UpdateBookingCommand;
import pipelines.data.BookingRepository;
import pipelines.domain.Booking;

@Component
public record UpdateBookingHandler(BookingRepository repository) implements Command.Handler<UpdateBookingCommand, Boolean> {
    @Override
    public Boolean handle(UpdateBookingCommand command) {
        Booking booking = new Booking(
                command.bookingId(),
                command.hotelName(),
                command.guestName(),
                command.email(),
                command.checkIn(),
                command.checkOut()
        );
        return repository.update(booking);
    }
}
