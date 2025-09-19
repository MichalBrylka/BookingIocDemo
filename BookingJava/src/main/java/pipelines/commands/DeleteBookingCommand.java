package pipelines.commands;

import an.awesome.pipelinr.Command;

import java.util.UUID;

public record DeleteBookingCommand(UUID bookingId) implements Command<Boolean> {}
