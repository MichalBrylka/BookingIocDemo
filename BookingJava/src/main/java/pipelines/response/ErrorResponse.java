package pipelines.response;

import java.util.Map;

record ErrorResponse(String title, int status, String type, Map<String, String> details) {
}