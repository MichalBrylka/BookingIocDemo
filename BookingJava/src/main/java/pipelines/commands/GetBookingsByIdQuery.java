package pipelines.commands;

import an.awesome.pipelinr.Command;
import pipelines.domain.Booking;

import java.util.UUID;

public record GetBookingsByIdQuery(UUID bookingId) implements Command<Booking> {}
