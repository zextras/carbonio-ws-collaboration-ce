package com.zextras.chats.core.exception.mapper;

import com.google.inject.Singleton;
import com.zextras.chats.core.exception.ChatsHttpException;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class ChatsHttpExceptionHandler extends ExceptionHandler implements ExceptionMapper<ChatsHttpException> {

  @Context
  private UriInfo uriInfo;

  @Inject
  public ChatsHttpExceptionHandler() {
  }

  @Override
  public UriInfo uriInfo() {
    return uriInfo;
  }

  @Override
  public Response toResponse(ChatsHttpException exception) {
    return handleException(exception, exception.getDebugInfo(), exception.getHttpStatus(), exception.isToLog());
  }


}
