package com.zextras.team.core.exception.mapper;

import com.google.inject.Singleton;
import com.zextras.team.core.exception.TeamHttpException;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class TeamHttpExceptionHandler extends ExceptionHandler implements ExceptionMapper<TeamHttpException> {

  @Context
  private UriInfo uriInfo;

  @Inject
  public TeamHttpExceptionHandler() {
  }

  @Override
  public UriInfo uriInfo() {
    return uriInfo;
  }

  @Override
  public Response toResponse(TeamHttpException exception) {
    return handleException(exception, exception.getDebugInfo(), exception.getHttpStatus(), exception.isToLog());
  }


}
