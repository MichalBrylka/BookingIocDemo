package pipelines.data;

import org.springframework.stereotype.Repository;
import pipelines.domain.Booking;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class InMemoryBookingRepository implements BookingRepository {
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

    @Override
    public void add(Booking booking) {
        booking.validate();
        bookings.put(booking.id(), booking);
    }

    @Override
    public List<Booking> get(Map<String, DataFilter> filter, Iterable<SortField> sort) {
        List<Booking> result = new ArrayList<>(bookings.values());

        // Apply filters only if filter != null
        if (filter != null) {
            Predicate<Booking> predicate = booking -> true;

            for (var kvp : filter.entrySet()) {
                predicate = predicate.and(switch (kvp.getValue()) {
                    case StringFilter sf -> stringPredicate(kvp.getKey(), sf);
                    case DateFilter df -> datePredicate(kvp.getKey(), df);
                });
            }

            result = result.stream().filter(predicate).collect(Collectors.toList());
        }

        // Apply sorting only if sort != null and not empty
        if (sort != null) {
            Iterator<SortField> it = sort.iterator();
            if (it.hasNext()) {
                Comparator<Booking> combined = null;
                for (SortField sf : sort) {
                    Comparator<Booking> comp = switch (sf.field()) {
                        case "hotelName" -> Comparator.comparing(Booking.getFieldAccessor("hotelName"), Comparator.nullsLast(String::compareTo));
                        case "guestName" -> Comparator.comparing(Booking.getFieldAccessor("guestName"), Comparator.nullsLast(String::compareTo));
                        case "email" -> Comparator.comparing(Booking.getFieldAccessor("email"), Comparator.nullsLast(String::compareTo));
                        case "checkIn" -> Comparator.comparing(Booking.getFieldAccessor("checkIn"), Comparator.nullsLast(LocalDate::compareTo));
                        case "checkOut" -> Comparator.comparing(Booking.getFieldAccessor("checkOut"), Comparator.nullsLast(LocalDate::compareTo));
                        default -> throw new IllegalArgumentException("Unknown sort field: " + sf.field());
                    };
                    if (!sf.ascending()) comp = comp.reversed();
                    combined = combined == null ? comp : combined.thenComparing(comp);
                }
                result.sort(combined);
            }
        }

        return result;
    }

    private Predicate<Booking> stringPredicate(String fieldName, StringFilter filter) {
        var getter = Booking.<String>getFieldAccessor(fieldName);

        return switch (filter.operator()) {
            case EQ -> b -> Objects.equals(getter.apply(b), filter.value());
            case NEQ -> b -> !Objects.equals(getter.apply(b), filter.value());
            case IN -> b -> getter.apply(b) != null && getter.apply(b).contains(filter.value());
            default -> throw new IllegalArgumentException("Unsupported operator for string field: " + filter.operator());
        };
    }

    private Predicate<Booking> datePredicate(String fieldName, DateFilter filter) {
        var getter = Booking.<LocalDate>getFieldAccessor(fieldName);

        return switch (filter.operator()) {
            case EQ -> b -> Objects.equals(getter.apply(b), filter.value());
            case NEQ -> b -> !Objects.equals(getter.apply(b), filter.value());
            case GT -> b -> getter.apply(b).isAfter(filter.value());
            case LT -> b -> getter.apply(b).isBefore(filter.value());
            case GTE -> b -> !getter.apply(b).isBefore(filter.value());
            case LTE -> b -> !getter.apply(b).isAfter(filter.value());
            default -> throw new IllegalArgumentException("Unsupported operator for date field: " + filter.operator());
        };
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
    public boolean patch(UUID bookingId, Map<?, ?> fields) {
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

    private static LocalDate getDateFromBody(Map<?, ?> fields, String fieldName) {
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