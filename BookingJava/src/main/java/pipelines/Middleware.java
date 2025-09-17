package pipelines;

import org.springframework.stereotype.Component;
import an.awesome.pipelinr.Command;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
@lombok.extern.slf4j.Slf4j
class LoggableMiddleware implements Command.Middleware {

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        log.debug("Executing command: {}", command);

        try {
            R response = next.invoke();
            log.debug("Response for {}: {}", command.getClass().getSimpleName(), response);
            return response;
        } catch (Exception ex) {
            log.error("Error executing {}: {}", command.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
    }
}

@Component
@Order(2)
@lombok.extern.slf4j.Slf4j
class DateValidationMiddleware implements Command.Middleware {
    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        if (command instanceof HasBookingDates hasDates) {
            var checkIn = hasDates.checkIn();
            var checkOut = hasDates.checkOut();

            if (checkIn != null && checkOut != null && checkIn.isAfter(checkOut))
                log.warn("Invalid booking dates: checkIn {} is after checkOut {}", checkIn, checkOut);
        }
        return next.invoke();
    }
}