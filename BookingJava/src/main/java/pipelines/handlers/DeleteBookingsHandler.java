package pipelines.handlers;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import pipelines.commands.DeleteBookingCommand;
import pipelines.data.BookingRepository;

@Component
public record DeleteBookingsHandler(BookingRepository repository) implements Command.Handler<DeleteBookingCommand, Boolean> {

    @Override
    public Boolean handle(DeleteBookingCommand deleteBookingCommand) {
        return repository.delete(deleteBookingCommand.bookingId());
    }
}
