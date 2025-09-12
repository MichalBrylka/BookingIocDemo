package pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.reflect.Type;

import java.util.*;

interface BookingRepository {
    void add(Booking booking);

    List<Booking> getAll();
}

class InMemoryBookingRepository implements BookingRepository {
    private final List<Booking> bookings = new ArrayList<>();

    public void add(Booking booking) {bookings.add(booking);}

    public List<Booking> getAll() {return Collections.unmodifiableList(bookings);}
}

class JacksonJsonMapper implements JsonMapper {

    private final ObjectMapper mapper;

    public JacksonJsonMapper() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // support LocalDate/LocalDateTime
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
            return new java.io.ByteArrayInputStream(mapper.writeValueAsBytes(obj));
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
