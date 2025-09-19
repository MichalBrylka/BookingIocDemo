package pipelines.data;

import pipelines.domain.Booking;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BookingRepository {
    UUID getNextId();

    void add(Booking booking);

    List<Booking> get(Map<String, DataFilter> filter, Iterable<SortField> sort);

    Booking getById(UUID bookingId);

    boolean delete(UUID bookingId);

    boolean update(Booking booking);

    boolean patch(UUID bookingId, Map<?, ?> fields);
}
