package pipelines.handlers;

import an.awesome.pipelinr.Command;
import org.springframework.stereotype.Component;
import pipelines.commands.BookHotelCommand;
import pipelines.commands.BookingConfirmedNotification;
import pipelines.data.BookingRepository;
import pipelines.domain.Booking;
import pipelines.infrastructure.NotificationPublisher;

import java.util.UUID;

@Component
public record BookHotelHandler(BookingRepository repository, NotificationPublisher publisher) implements Command.Handler<BookHotelCommand, UUID> {
    @Override
    public UUID handle(BookHotelCommand command) {
        var booking = new Booking(repository.getNextId(), command.hotelName(), command.guestName(), command.email(), command.checkIn(), command.checkOut());
        repository.add(booking);
        publisher.publish(new BookingConfirmedNotification(booking));
        return booking.id();
    }
}
