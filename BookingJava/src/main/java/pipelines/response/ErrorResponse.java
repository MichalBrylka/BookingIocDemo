package pipelines.response;

import java.util.Map;

//TODO use that like here: https://github.com/javalin/javalin-samples/blob/main/javalin6/javalin-openapi-example/src/main/java/io/javalin/example/java/user/UserController.java
record ErrorResponse(String title, int status, String type, Map<String, String> details) {
}