using BookingNet;
using MediatR;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;


var services = new ServiceCollection();

services.AddLogging(builder =>
{
    builder.ClearProviders();
    builder.AddSimpleConsole(options =>
    {
        options.SingleLine = true;
        options.TimestampFormat = "[HH:mm:ss] ";
        options.ColorBehavior = Microsoft.Extensions.Logging.Console.LoggerColorBehavior.Default;
    });

    // Set minimum log level globally
    builder.SetMinimumLevel(LogLevel.Information);

    // You can override per-category if you want:
    builder.AddFilter("HotelBookingApp", LogLevel.Information);
    builder.AddFilter("MediatR", LogLevel.Warning);
});

services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(Program).Assembly));
services.AddSingleton<IBookingRepository, InMemoryBookingRepository>();
services.AddTransient(typeof(IPipelineBehavior<,>), typeof(LoggingBehavior<,>));

var provider = services.BuildServiceProvider();
var mediator = provider.GetRequiredService<IMediator>();

// Make some bookings
var bookingId = await mediator.Send(new BookHotelCommand("Grand Hotel", "Alice", DateTime.Today, DateTime.Today.AddDays(2)));



await mediator.Send(new BookHotelCommand("Seaside Resort", "Bob", DateTime.Today.AddDays(1), DateTime.Today.AddDays(3)));

// Fetch all bookings
var bookings = await mediator.Send(new GetBookingsQuery());
Console.WriteLine("\nAll bookings:");
foreach (var booking in bookings)
    Console.WriteLine($" - {booking.HotelName} for {booking.GuestName} ({booking.CheckIn:yyyy-MM-dd} to {booking.CheckOut:yyyy-MM-dd})");