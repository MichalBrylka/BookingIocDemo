package pipelines.handlers;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import pipelines.commands.PatchBookingCommand;
import pipelines.data.BookingRepository;

@Component
public record PatchBookingHandler(BookingRepository repository) implements Command.Handler<PatchBookingCommand, Boolean> {
    @Override
    public Boolean handle(PatchBookingCommand command) {
        return repository.patch(command.bookingId(), command.fields());
    }
}
