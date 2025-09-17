package pipelines;

import org.springframework.stereotype.Component;
import an.awesome.pipelinr.Command;
import org.springframework.core.annotation.Order;

@Component
@Order(1)
class Loggable implements Command.Middleware {

    @Override
    public <R, C extends Command<R>> R invoke(C c, Next<R> next) {
        return null;
    }
}