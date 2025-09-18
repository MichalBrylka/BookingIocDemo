package pipelines;

import an.awesome.pipelinr.Command;
import an.awesome.pipelinr.Notification;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
record BookHotelHandler(BookingRepository repository, NotificationPublisher publisher) implements Command.Handler<BookHotelCommand, UUID> {
    @Override
    public UUID handle(BookHotelCommand command) {
        var booking = new Booking(repository.getNextId(), command.hotelName(), command.guestName(),command.email(), command.checkIn(), command.checkOut());
        repository.add(booking);
        publisher.publish(new BookingConfirmedNotification(booking));
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
record GetBookingsByIdHandler(BookingRepository repository) implements Command.Handler<GetBookingsByIdQuery, Booking> {
    @Override
    public Booking handle(GetBookingsByIdQuery query) {
        return repository.getById(query.bookingId());
    }
}

@Component
record DeleteBookingsHandler(BookingRepository repository) implements Command.Handler<DeleteBookingCommand, Boolean> {

    @Override
    public Boolean handle(DeleteBookingCommand deleteBookingCommand) {
        return repository.delete(deleteBookingCommand.bookingId());
    }
}

@Component
record UpdateBookingHandler(BookingRepository repository) implements Command.Handler<UpdateBookingCommand, Boolean> {
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

@Component
record PatchBookingHandler(BookingRepository repository) implements Command.Handler<PatchBookingCommand, Boolean> {
    @Override
    public Boolean handle(PatchBookingCommand command) {
        return repository.patch(command.bookingId(), command.fields());
    }
}

@Component
record SendConfirmationEmailHandler(EmailService emailService) implements Notification.Handler<BookingConfirmedNotification> {
    @Override
    public void handle(BookingConfirmedNotification notification) {
        var booking = notification.booking();
        emailService.sendEmail(booking.email(), "Booking Confirmed",
                String.format("Dear %s, your booking at %s is confirmed. Booking ID: %s", booking.guestName(), booking.hotelName(), booking.id()));
    }
}

@Component
class UpdateInventoryHandler implements Notification.Handler<BookingConfirmedNotification> {
    @Override
    public void handle(BookingConfirmedNotification notification) {
        var booking = notification.booking();
        System.out.printf("🏨 Updating inventory for hotel %s (check-in: %s, check-out: %s)%n",
                booking.hotelName(),
                booking.checkIn(),
                booking.checkOut());
    }
}
