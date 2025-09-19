package pipelines;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pipelines.data.DataFilter;
import pipelines.data.InMemoryBookingRepository;
import pipelines.data.*;
import pipelines.domain.Booking;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryBookingRepositoryTest {
    private static InMemoryBookingRepository repository() {
        var bookings = Stream.of(
                new Booking(new UUID(0L, 1L), "Hilton", "Alice", "alice@example.com", LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 15)),
                new Booking(new UUID(0L, 2L), "Marriott", "Bob", "bob@example.com", LocalDate.of(2024, 6, 12), LocalDate.of(2024, 6, 18)),
                new Booking(new UUID(0L, 3L), "Hilton", "Charlie", "charlie@example.com", LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 20))
        ).collect(Collectors.toMap(Booking::id, b -> b, (a, b) -> a, HashMap::new));

        return new InMemoryBookingRepository(bookings);
    }

    private static Stream<Arguments> provideFiltersAndSorts() {
        return Stream.of(
                Arguments.of("No filter, no sort", null, null, 3),

                Arguments.of("Filter hotelName eq Hilton",
                        Map.of("hotelName", new StringFilter("Hilton", Operator.EQ)),
                        null,
                        2
                ),

                Arguments.of("Filter guestName eq Bob",
                        Map.of("guestName", new StringFilter("Bob", Operator.EQ)),
                        null,
                        1
                ),

                Arguments.of("Filter checkOut lte 2024-06-18",
                        Map.of("checkOut", new DateFilter(LocalDate.of(2024, 6, 18), Operator.LTE)),
                        null,
                        2
                ),

                Arguments.of("Filter hotelName eq Hilton, sort by checkIn DESC",
                        Map.of("hotelName", new StringFilter("Hilton", Operator.EQ)),
                        List.of(new SortField("checkIn", false)),
                        2
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideFiltersAndSorts")
    void testGetAll(String ignored, Map<String, DataFilter<?>> filter, Iterable<SortField> sort, int expectedCount) {
        InMemoryBookingRepository repo = repository();
        List<Booking> result = repo.get(filter, sort);
        assertThat(result).hasSize(expectedCount);

        // Optional: verify sorting if sort is not empty (only check first sort field for simplicity)
        if (sort != null) {
            SortField sf = sort.iterator().next();

            if (sf.field().startsWith("check")) {
                var dates = result.stream().map(Booking.<LocalDate>getFieldAccessor(sf.field())).toList();
                if (sf.ascending())
                    assertThat(dates).isSorted();
                else
                    assertThat(dates).isSortedAccordingTo(Comparator.reverseOrder());
            } else {
                var texts = result.stream().map(Booking.<String>getFieldAccessor(sf.field())).toList();
                if (sf.ascending())
                    assertThat(texts).isSorted();
                else
                    assertThat(texts).isSortedAccordingTo(Comparator.reverseOrder());
            }
        }
    }
}