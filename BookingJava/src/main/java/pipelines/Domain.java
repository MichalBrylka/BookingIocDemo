package pipelines;

import java.time.LocalDate;
import java.util.UUID;

record Booking(
        UUID id,
        String hotelName,
        String guestName,
        String email,
        LocalDate checkIn,
        LocalDate checkOut) {

    public Booking validate() {
        if (hotelName == null || hotelName.isBlank())
            throw new IllegalArgumentException("hotelName is required");

        if (guestName == null || guestName.isBlank())
            throw new IllegalArgumentException("guestName is required");

        if (email == null || email.isBlank() || !email.contains("@"))
            throw new IllegalArgumentException("A valid email is required");

        if (checkIn == null)
            throw new IllegalArgumentException("checkIn is required");
        if (checkOut == null)
            throw new IllegalArgumentException("checkOut is required");

        if (checkIn.isAfter(checkOut))
            throw new IllegalArgumentException("checkIn cannot be after checkOut");

        return this;
    }
}
