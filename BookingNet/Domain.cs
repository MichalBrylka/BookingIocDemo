namespace BookingNet;

public record Booking(string HotelName, string GuestName, DateTime CheckIn, DateTime CheckOut, Guid Id)
{
    public Booking(string hotelName, string guestName, DateTime checkIn, DateTime checkOut) : this(hotelName, guestName, checkIn, checkOut, Guid.NewGuid()) { }
}
