package pipelines.commands;

import an.awesome.pipelinr.Notification;
import pipelines.domain.Booking;

public record BookingConfirmedNotification(Booking booking) implements Notification {}
