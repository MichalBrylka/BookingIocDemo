namespace BookingNet;

public interface IBookingRepository
{
    void Add(Booking booking);
    IEnumerable<Booking> GetAll();
}

class InMemoryBookingRepository : IBookingRepository
{
    private readonly List<Booking> _bookings = [];

    public void Add(Booking booking) => _bookings.Add(booking);
    public IEnumerable<Booking> GetAll() => _bookings;
}