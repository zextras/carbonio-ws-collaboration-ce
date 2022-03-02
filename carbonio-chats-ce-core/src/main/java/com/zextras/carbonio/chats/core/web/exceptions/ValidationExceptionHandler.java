package com.zextras.carbonio.chats.core.web.exceptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class ValidationExceptionHandler extends ExceptionHandler<ValidationException> {

  @Inject
  public ValidationExceptionHandler() {
  }

  @Override
  public Response toResponse(ValidationException exception) {
    return handleException(exception, exception.getMessage(),
      Status.BAD_REQUEST, false);
  }
}
