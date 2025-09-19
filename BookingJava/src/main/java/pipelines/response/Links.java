package pipelines.response;

public record Links(Link self, Link update, Link patch, Link delete, Link list) {}
