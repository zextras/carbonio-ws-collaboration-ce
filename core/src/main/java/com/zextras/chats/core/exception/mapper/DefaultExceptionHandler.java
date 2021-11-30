package com.zextras.chats.core.exception.mapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Singleton
@Provider
public class DefaultExceptionHandler extends ExceptionHandler implements ExceptionMapper<Exception> {

  @Context
  private UriInfo uriInfo;

  @Inject
  public DefaultExceptionHandler() {
  }

  @Override
  public UriInfo uriInfo() {
    return uriInfo;
  }

  @Override
  public Response toResponse(Exception exception) {
    return handleException(exception, exception.getMessage(), Status.INTERNAL_SERVER_ERROR, true);
  }
}