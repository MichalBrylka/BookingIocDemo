package pipelines.infrastructure;

import org.springframework.stereotype.Component;
import an.awesome.pipelinr.Command;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
@lombok.extern.slf4j.Slf4j
public class LoggableMiddleware implements Command.Middleware {

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
