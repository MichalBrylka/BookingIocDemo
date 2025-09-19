package pipelines.handlers;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import pipelines.commands.GetBookingsQuery;
import pipelines.data.BookingRepository;
import pipelines.domain.Booking;

import java.util.List;

@Component
public record GetBookingsHandler(BookingRepository repository) implements Command.Handler<GetBookingsQuery, List<Booking>> {
    @Override
    public List<Booking> handle(GetBookingsQuery query) {
        return repository.get(query.filter(), query.sort());
    }
}
