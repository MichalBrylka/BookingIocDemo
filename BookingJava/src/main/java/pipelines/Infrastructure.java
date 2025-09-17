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
    void add(Booking booking);

    List<Booking> getAll();

    boolean delete(UUID bookingId);

    boolean update(Booking booking);

    boolean patch(UUID bookingId, Map<String, Object> fields);
}

@Repository
class InMemoryBookingRepository implements BookingRepository {
    private final List<Booking> bookings;

    public InMemoryBookingRepository() {
        this(null);
    }

    public InMemoryBookingRepository(List<Booking> bookings) {
        this.bookings = bookings == null ? new ArrayList<>() : bookings;
    }

    public void add(Booking booking) {
        bookings.add(booking);
    }

    public List<Booking> getAll() {return Collections.unmodifiableList(bookings);}

    @Override
    public boolean delete(UUID bookingId) {
        return bookings.removeIf(b -> b.id().equals(bookingId));
    }

    @Override
    public boolean update(Booking booking) {
        for (int i = 0; i < bookings.size(); i++) {
            if (bookings.get(i).id().equals(booking.id())) {
                bookings.set(i, booking);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean patch(UUID bookingId, Map<String, Object> fields) {
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            if (b.id().equals(bookingId)) {
                String hotelName = fields.containsKey("hotelName") ? (String) fields.get("hotelName") : b.hotelName();
                String guestName = fields.containsKey("guestName") ? (String) fields.get("guestName") : b.guestName();
                LocalDate checkIn = fields.containsKey("checkIn") ? LocalDate.parse((String) fields.get("checkIn")) : b.checkIn();
                LocalDate checkOut = fields.containsKey("checkOut") ? LocalDate.parse((String) fields.get("checkOut")) : b.checkOut();
                bookings.set(i, new Booking(b.id(), hotelName, guestName, checkIn, checkOut));
                return true;
            }
        }
        return false;
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
