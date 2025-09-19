package pipelines.handlers;

import an.awesome.pipelinr.Notification;
import org.springframework.stereotype.Component;
import pipelines.commands.BookingConfirmedNotification;
import pipelines.infrastructure.EmailService;

@Component
public record SendConfirmationEmailHandler(EmailService emailService) implements Notification.Handler<BookingConfirmedNotification> {
    @Override
    public void handle(BookingConfirmedNotification notification) {
        var booking = notification.booking();
        emailService.sendEmail(booking.email(), "Booking Confirmed",
                String.format("Dear %s, your booking at %s is confirmed. Booking ID: %s", booking.guestName(), booking.hotelName(), booking.id()));
    }
}
