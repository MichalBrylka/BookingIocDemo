package pipelines;

import io.javalin.openapi.OpenApiIgnore;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;

record Booking(
        UUID id,
        String hotelName,
        String guestName,
        String email,
        LocalDate checkIn,
        LocalDate checkOut) {

    @SuppressWarnings("unchecked")
    public static <R> Function<Booking, R> getFieldAccessor(String fieldName) {
        return (Function<Booking, R>) switch (fieldName) {
            case "id" -> (Function<Booking, UUID>) Booking::id;
            case "hotelName" -> (Function<Booking, String>) Booking::hotelName;
            case "guestName" -> (Function<Booking, String>) Booking::guestName;
            case "email" -> (Function<Booking, String>) Booking::email;
            case "checkIn" -> (Function<Booking, LocalDate>) Booking::checkIn;
            case "checkOut" -> (Function<Booking, LocalDate>) Booking::checkOut;
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
    }

    @OpenApiIgnore
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
