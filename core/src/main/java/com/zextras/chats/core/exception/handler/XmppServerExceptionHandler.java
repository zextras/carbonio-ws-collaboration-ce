package com.zextras.chats.core.exception.handler;

import com.google.inject.Singleton;
import com.zextras.chats.mongooseim.admin.invoker.ApiException;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class XmppServerExceptionHandler extends ExceptionHandler<ApiException> {

  @Context
  private UriInfo uriInfo;

  @Inject
  public XmppServerExceptionHandler() {
  }

  @Override
  public UriInfo uriInfo() {
    return uriInfo;
  }

  @Override
  public Response toResponse(ApiException exception) {
    return handleException(exception, exception.getMessage(), Status.fromStatusCode(exception.getCode()), true);
  }
}
