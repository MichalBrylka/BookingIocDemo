package pipelines.infrastructure;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;

class JacksonJsonMapper implements JsonMapper {

    private final ObjectMapper mapper;

    public JacksonJsonMapper() {
        mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .defaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS))
                .addModule(new JavaTimeModule())// support JDK 8 date/time types, etc.
                .build();
    }

    @Override
    public <T> @NotNull T fromJsonStream(@NotNull InputStream inputStream, @NotNull Type targetType) {
        try {
            return mapper.readValue(inputStream, mapper.constructType(targetType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> @NotNull T fromJsonString(@NotNull String json, @NotNull Type targetType) {
        try {
            return mapper.readValue(json, mapper.constructType(targetType));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull InputStream toJsonStream(@NotNull Object obj, @NotNull Type type) {
        try {
            return new ByteArrayInputStream(mapper.writeValueAsBytes(obj));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull String toJsonString(@NotNull Object obj, @NotNull Type type) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
