package pipelines.commands;

import an.awesome.pipelinr.Command;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateBookingCommand(UUID bookingId, String hotelName, String guestName, String email, LocalDate checkIn,
                                   LocalDate checkOut) implements Command<Boolean> {}
