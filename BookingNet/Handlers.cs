using MediatR;

namespace BookingNet;

public class BookHotelCommandHandler(IBookingRepository repository) : IRequestHandler<BookHotelCommand, Guid>
{
    public Task<Guid> Handle(BookHotelCommand request, CancellationToken cancellationToken)
    {
        var booking = new Booking(request.HotelName, request.GuestName, request.CheckIn, request.CheckOut);
        repository.Add(booking);
        return Task.FromResult(booking.Id);
    }
}

public class GetBookingsQueryHandler(IBookingRepository repository) : IRequestHandler<GetBookingsQuery, IEnumerable<Booking>>
{
    public Task<IEnumerable<Booking>> Handle(GetBookingsQuery request, CancellationToken cancellationToken) => Task.FromResult(
        request.Predicate is { } predicate
        ? repository.GetAll().Where(predicate)
        : repository.GetAll()
        );
}