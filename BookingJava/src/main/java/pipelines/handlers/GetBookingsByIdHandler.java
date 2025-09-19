package pipelines.handlers;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import pipelines.commands.GetBookingsByIdQuery;
import pipelines.data.BookingRepository;
import pipelines.domain.Booking;

@Component
public record GetBookingsByIdHandler(BookingRepository repository) implements Command.Handler<GetBookingsByIdQuery, Booking> {
    @Override
    public Booking handle(GetBookingsByIdQuery query) {
        return repository.getById(query.bookingId());
    }
}
