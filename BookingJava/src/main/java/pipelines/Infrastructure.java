package pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;

import java.time.LocalDate;
import java.util.*;

interface BookingRepository {
    UUID getNextId();

    void add(Booking booking);

    List<Booking> getAll();

    Booking getById(UUID bookingId);

    boolean delete(UUID bookingId);

    boolean update(Booking booking);

    boolean patch(UUID bookingId, Map<String, Object> fields);
}

@Repository
class InMemoryBookingRepository implements BookingRepository {
    private final Map<UUID, Booking> bookings;

    public InMemoryBookingRepository() {
        this(null);
    }

    public InMemoryBookingRepository(Map<UUID, Booking> bookings) {
        this.bookings = bookings == null ? new HashMap<>() : bookings;
    }

    @Override
    public UUID getNextId() {
        return UUID.randomUUID();
    }

    public void add(Booking booking) {
        booking.validate();
        bookings.put(booking.id(), booking);
    }

    public List<Booking> getAll() {
        return List.copyOf(bookings.values());
    }

    @Override
    public Booking getById(UUID bookingId) {
        return bookings.get(bookingId);
    }

    @Override
    public boolean delete(UUID bookingId) {
        return bookings.remove(bookingId) != null;
    }

    @Override
    public boolean update(Booking booking) {
        booking.validate();
        if (bookings.containsKey(booking.id())) {
            bookings.put(booking.id(), booking);
            return true;
        }
        return false;
    }

    @Override
    public boolean patch(UUID bookingId, Map<String, Object> fields) {
        Booking old = bookings.get(bookingId);
        if (old == null) return false;

        String hotelName = fields.containsKey("hotelName") ? (String) fields.get("hotelName") : old.hotelName();
        String guestName = fields.containsKey("guestName") ? (String) fields.get("guestName") : old.guestName();
        String email = fields.containsKey("email") ? (String) fields.get("email") : old.email();

        LocalDate checkIn = fields.containsKey("checkIn") ? getDateFromBody(fields, "checkIn") : old.checkIn();
        LocalDate checkOut = fields.containsKey("checkOut") ? getDateFromBody(fields, "checkOut") : old.checkOut();

        bookings.put(bookingId, new Booking(bookingId, hotelName, guestName, email, checkIn, checkOut).validate());
        return true;
    }

    private static LocalDate getDateFromBody(Map<String, Object> fields, String fieldName) {
        try {
            var dateStr = (String) fields.get(fieldName);
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Expected ISO-8601 (yyyy-MM-dd) format for field %s: %s"
                            .formatted(fieldName, e.getMessage())
            );
        }
    }
}

interface EmailService {
    void sendEmail(String to, String subject, String body);
}

@Component
class ConsoleLoggingEmailService implements EmailService {
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.printf("Sending email to: '%s' with subject '%s' and body '%s'  %n", to, subject, body);
    }
}

@Component
class JacksonJsonMapper implements JsonMapper {

    private final ObjectMapper mapper;

    public JacksonJsonMapper() {
        mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS))
                .addModule(new JavaTimeModule())// support JDK 8 date/time types, etc.
                .build();
    }

    @Override
    public <T> @NotNull T fromJsonStream(@NotNull InputStream inputStream, @NotNull Type targetType) {
        try {
            return mapper.readValue(inputStream, mapper.constructType(targetType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> @NotNull T fromJsonString(@NotNull String json, @NotNull Type targetType) {
        try {
            return mapper.readValue(json, mapper.constructType(targetType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull InputStream toJsonStream(@NotNull Object obj, @NotNull Type type) {
        try {
            return new ByteArrayInputStream(mapper.writeValueAsBytes(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull String toJsonString(@NotNull Object obj, @NotNull Type type) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
