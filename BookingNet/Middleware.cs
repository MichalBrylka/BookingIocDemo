using MediatR;
using Microsoft.Extensions.Logging;

namespace BookingNet;

public class LoggingBehavior<TRequest, TResponse>(ILogger<LoggingBehavior<TRequest, TResponse>> logger) : IPipelineBehavior<TRequest, TResponse>
    where TRequest : notnull
{
    public async Task<TResponse> Handle(TRequest request, RequestHandlerDelegate<TResponse> next, CancellationToken cancellationToken)
    {
        logger.LogInformation("Handling {RequestName} with payload: {@Request}", typeof(TRequest).Name, request);

        var response = await next(cancellationToken);

        logger.LogInformation("Handled {RequestName}, response: {@Response}", typeof(TRequest).Name, response);

        return response;
    }
}