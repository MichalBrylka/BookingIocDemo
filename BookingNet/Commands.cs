using MediatR;

namespace BookingNet;

public record GetBookingsQuery(Func<Booking, bool>? Predicate=null) : IRequest<IEnumerable<Booking>>;

public record BookHotelCommand(string HotelName, string GuestName, DateTime CheckIn, DateTime CheckOut) : IRequest<Guid>;
