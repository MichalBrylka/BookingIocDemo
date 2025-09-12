package pipelines;

import an.awesome.pipelinr.Command;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelinrCommandArchTest {

    private static final String BASE_PACKAGE = "pipelines";

    @Test
    void everyCommandShouldHaveAHandler() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // All command implementations (excluding the interface itself)
        var commands = classes.stream()
                .filter(c -> c.isAssignableTo(Command.class.getName())
                             && !c.getName().equals(Command.class.getName()))
                .collect(Collectors.toSet());

        var handlers = classes.stream()
                .filter(c ->
                        c.getInterfaces().stream()
                                .anyMatch(i -> i.toErasure().getName().equals(Command.Handler.class.getName()))
                ).collect(Collectors.toSet());

        Map<String, List<JavaClass>> handlersByCommand = handlers.stream()
                .collect(Collectors.groupingBy(
                        handler -> extractCommandType(handler).getName()
                ));

        List<String> missingHandlers = new ArrayList<>();

        for (JavaClass command : commands)
            if (!handlersByCommand.containsKey(command.getName()))
                missingHandlers.add(command.getFullName());

        assertThat(missingHandlers)
                .as("Every Command should have a matching Handler")
                .isEmpty();
    }

    private JavaClass extractCommandType(JavaClass handler) {
        return handler.getInterfaces().stream()
                .filter(i -> i.toErasure().getName().equals(Command.Handler.class.getName()))
                .findFirst()
                .flatMap(i -> i instanceof JavaParameterizedType
                        ? Optional.of(((JavaParameterizedType) i).getActualTypeArguments().get(0).toErasure())
                        : Optional.empty())
                .orElseThrow(() ->
                        new IllegalStateException("Handler without generic command type: " + handler.getFullName()));
    }
}
