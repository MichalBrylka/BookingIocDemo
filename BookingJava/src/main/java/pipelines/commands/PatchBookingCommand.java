package pipelines.commands;

import an.awesome.pipelinr.Command;

import java.util.Map;
import java.util.UUID;

public record PatchBookingCommand(UUID bookingId, Map<?, ?> fields) implements Command<Boolean> {}
