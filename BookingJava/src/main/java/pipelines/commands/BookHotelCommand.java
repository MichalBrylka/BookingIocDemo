package pipelines.commands;

import an.awesome.pipelinr.Command;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record BookHotelCommand(
        @NotNull String hotelName, @NotNull String guestName, @NotNull String email,
        @NotNull LocalDate checkIn, @NotNull LocalDate checkOut) implements Command<UUID> {}
