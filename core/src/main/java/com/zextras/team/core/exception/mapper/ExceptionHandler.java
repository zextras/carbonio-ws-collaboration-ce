package com.zextras.team.core.exception.mapper;

import com.zextras.team.core.logging.TeamLogger;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;


public abstract class ExceptionHandler {

  private static final String APPLICATION_PACKAGE = "com.zextras.team";

  public abstract UriInfo uriInfo();

  public Response handleException(Exception exception, String msg, Status status, boolean isToLog) {
    if (isToLog) {
      TeamLogger.error(String.format("An error occurred on %s at %s", getRequestUri(), getExceptionPosition(exception)),
        exception);
    }
    return Response.status(status).build();
  }


  private String getExceptionPosition(Exception exception) {
    StackTraceElement[] stackTrace = exception.getStackTrace();
    Optional<StackTraceElement> stackTraceElement = Arrays.stream(stackTrace).findFirst();
    return stackTraceElement.map(
        element -> String.format("%s.%s [%d]", element.getClassName(), element.getMethodName(), element.getLineNumber()))
      .orElse("");
  }

  private String getRequestUri() {
    if (uriInfo() == null) {
      return "Unable to find request uri";
    } else {
      String path = uriInfo().getAbsolutePath().getPath();
      if (uriInfo().getQueryParameters() != null && uriInfo().getQueryParameters().size() > 0) {
        path += "?" + uriInfo().getQueryParameters().entrySet().stream().flatMap((param) ->
          param.getValue().stream().map(paramValue -> String.join("=", param.getKey(), paramValue))
        ).collect(Collectors.joining("&"));
      }
      return path;
    }
  }
}
