// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class ExceptionHandler<E extends Throwable> implements ExceptionMapper<E> {

  @Inject private AppConfig appConfig;

  @Context private UriInfo uriInfo;

  public Response handleException(
      Exception exception,
      String msg,
      int httpStatusCode,
      String httpStatusPhrase,
      boolean isToLog) {
    if (isToLog) {
      ChatsLogger.error(
          String.format(
              "An error occurred on %s %n at %s %n %s",
              getRequestUri(), getExceptionPosition(exception), msg));
    }
    return Response.status(httpStatusCode).type(httpStatusPhrase).build();
  }

  private String getExceptionPosition(Exception exception) {
    StackTraceElement[] stackTrace = exception.getStackTrace();
    Optional<StackTraceElement> stackTraceElement = Arrays.stream(stackTrace).findFirst();
    return stackTraceElement
        .map(
            element ->
                String.format(
                    "%s.%s [line %d]",
                    element.getClassName(), element.getMethodName(), element.getLineNumber()))
        .orElse("");
  }

  private String getRequestUri() {
    if (uriInfo == null) {
      return "Unable to find request uri";
    } else {
      String path = uriInfo.getAbsolutePath().getPath();
      if (uriInfo.getQueryParameters() != null && !uriInfo.getQueryParameters().isEmpty()) {
        path +=
            "?"
                + uriInfo.getQueryParameters().entrySet().stream()
                    .flatMap(
                        (param) ->
                            param.getValue().stream()
                                .map(paramValue -> String.join("=", param.getKey(), paramValue)))
                    .collect(Collectors.joining("&"));
      }
      return path;
    }
  }
}
