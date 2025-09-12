package pipelines;

import an.awesome.pipelinr.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(IoC.class);

        var app = context.getBean(io.javalin.Javalin.class);
        app.start(8080);
        System.out.println("🚀 Server running at http://localhost:" + app.port());
    }
}
