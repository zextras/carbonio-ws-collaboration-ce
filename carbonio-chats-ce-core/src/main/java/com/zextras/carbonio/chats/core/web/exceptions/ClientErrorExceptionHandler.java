package com.zextras.carbonio.chats.core.web.exceptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class ClientErrorExceptionHandler extends ExceptionHandler<ClientErrorException> {

  @Inject
  public ClientErrorExceptionHandler() {
  }

  @Override
  public Response toResponse(ClientErrorException exception) {
    return handleException(exception, exception.getMessage(), exception.getResponse().getStatus(),
      Status.fromStatusCode(exception.getResponse().getStatus()).getReasonPhrase(), false);
  }
}
