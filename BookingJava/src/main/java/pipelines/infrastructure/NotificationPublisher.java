package pipelines.infrastructure;

import an.awesome.pipelinr.Notification;
import an.awesome.pipelinr.Pipeline;
import org.springframework.stereotype.Service;

@Service
public class NotificationPublisher {
    private final Pipeline pipeline;

    public NotificationPublisher(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    public void publish(Notification notification) {
        pipeline.send(notification);
    }
}