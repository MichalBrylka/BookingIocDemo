package pipelines.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataExpressionParser {
    public static Iterable<SortField> parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) return null;

        List<SortField> sortFields = new ArrayList<>();
        for (String part : sortParam.split(",")) {
            String[] tokens = part.trim().split("\\s+");
            String field = tokens[0].trim();
            boolean descending = tokens.length == 2 && tokens[1].equalsIgnoreCase("DESC");
            sortFields.add(new SortField(field, !descending));
        }
        return sortFields;
    }

    // regex: field operator 'value', value can have '' escaped quote
    private static final Pattern CONDITION_PATTERN = Pattern.compile(
            "(\\w+)\\s+(eq|neq|has|gt|lt|gte|lte)\\s+'((?:[^']|'')*)'",
            Pattern.CASE_INSENSITIVE
    );

    //TODO make generic to recognize field types from T
    public static <T> Map<String, DataFilter<?>> parseFilter(String filterParam, Class<T> clazz) {
        if (filterParam == null || filterParam.isBlank()) return null;

        var result = new java.util.HashMap<String, DataFilter<?>>();


        String[] conditions = filterParam.split("(?i)\\s+AND\\s+");// Split by AND (case-insensitive)
        for (String condition : conditions) {
            Matcher matcher = CONDITION_PATTERN.matcher(condition.trim());
            if (!matcher.matches()) continue; // skip invalid

            String field = matcher.group(1);
            String op = matcher.group(2);
            String valueRaw = matcher.group(3).replace("''", "'"); // unescape ''

            Class<?> fieldType;
            try {
                fieldType = clazz.getDeclaredField(field).getType();
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Unknown field: " + field);
            }

            if (fieldType.equals(String.class))
                result.put(field, new StringFilter(valueRaw, Operator.fromString(op)));
            else if (fieldType.equals(LocalDate.class))
                result.put(field, new DateFilter(LocalDate.parse(valueRaw), Operator.fromString(op)));
            else if (fieldType.equals(UUID.class))
                result.put(field, new UuidFilter(UUID.fromString(valueRaw), Operator.fromString(op)));
            else
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
        return result;
    }
}
