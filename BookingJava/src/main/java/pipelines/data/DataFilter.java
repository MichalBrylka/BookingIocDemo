package pipelines.data;

public sealed interface DataFilter permits DateFilter, StringFilter, UuidFilter {
}
