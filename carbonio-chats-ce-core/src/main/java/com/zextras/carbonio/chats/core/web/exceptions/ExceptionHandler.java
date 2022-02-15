// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.exceptions;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.model.ErrorDto;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import org.apache.commons.lang3.RandomStringUtils;


public abstract class ExceptionHandler<E extends Throwable> implements ExceptionMapper<E> {

  @Inject
  private AppConfig appConfig;

  @Context
  private UriInfo uriInfo;

  public Response handleException(Exception exception, String msg, Status status, boolean isToLog) {
    if (isToLog) {
      ChatsLogger.error(
        String.format("An error occurred on %s at %s", getRequestUri(), getExceptionPosition(exception)),
        exception
      );
    }
    ResponseBuilder responseBuilder = Response.status(status);
    if (EnvironmentType.DEVELOPMENT.equals(appConfig.getEnvType())) {
      ErrorDto error = new ErrorDto();
      error.setTraceId(RandomStringUtils.randomAlphabetic(16));
      error.setMessage(msg);
      responseBuilder.entity(error);
    }
    return responseBuilder.build();
  }


  private String getExceptionPosition(Exception exception) {
    StackTraceElement[] stackTrace = exception.getStackTrace();
    Optional<StackTraceElement> stackTraceElement = Arrays.stream(stackTrace).findFirst();
    return stackTraceElement.map(
        element -> String.format("%s.%s [line %d]", element.getClassName(), element.getMethodName(),
          element.getLineNumber()))
      .orElse("");
  }

  private String getRequestUri() {
    if (uriInfo == null) {
      return "Unable to find request uri";
    } else {
      String path = uriInfo.getAbsolutePath().getPath();
      if (uriInfo.getQueryParameters() != null && uriInfo.getQueryParameters().size() > 0) {
        path += "?" + uriInfo.getQueryParameters().entrySet().stream().flatMap((param) ->
          param.getValue().stream().map(paramValue -> String.join("=", param.getKey(), paramValue))
        ).collect(Collectors.joining("&"));
      }
      return path;
    }
  }
}
