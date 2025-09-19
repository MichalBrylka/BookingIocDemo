package pipelines.handlers;

import an.awesome.pipelinr.Notification;
import org.springframework.stereotype.Component;
import pipelines.commands.*;

import java.util.*;

@Component
public class UpdateInventoryHandler implements Notification.Handler<BookingConfirmedNotification> {
    @Override
    public void handle(BookingConfirmedNotification notification) {
        var booking = notification.booking();
        System.out.printf("🏨 Updating inventory for hotel %s (check-in: %s, check-out: %s)%n",
                booking.hotelName(),
                booking.checkIn(),
                booking.checkOut());
    }
}
