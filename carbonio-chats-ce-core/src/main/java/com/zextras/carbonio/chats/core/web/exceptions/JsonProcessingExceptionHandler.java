package com.zextras.carbonio.chats.core.web.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class JsonProcessingExceptionHandler extends ExceptionHandler<JsonProcessingException> {

  @Inject
  public JsonProcessingExceptionHandler() {
  }

  @Override
  public Response toResponse(JsonProcessingException exception) {
    return handleException(exception, exception.getMessage(), Status.BAD_REQUEST, false);
  }
}
