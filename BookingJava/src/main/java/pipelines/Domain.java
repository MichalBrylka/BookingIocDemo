package pipelines;

import java.time.LocalDate;
import java.util.UUID;

record Booking(UUID id, String hotelName, String guestName, LocalDate checkIn, LocalDate checkOut) {}
