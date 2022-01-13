package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.core.api.HealthApiService;
import com.zextras.carbonio.chats.core.api.NotFoundException;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

public class HealthApiServiceImpl implements HealthApiService {

  private final HealthcheckService healthcheckService;

  @Inject
  public HealthApiServiceImpl(HealthcheckService healthcheckService) {
    this.healthcheckService = healthcheckService;
  }

  @Override
  public Response healthInfo(SecurityContext securityContext) throws NotFoundException {
    return Response.ok()
      .entity(healthcheckService.getServiceHealth())
      .build();
  }

  @Override
  public Response isLive(SecurityContext securityContext) throws NotFoundException {
    return Response.noContent().build();
  }

  @Override
  public Response isReady(SecurityContext securityContext) throws NotFoundException {
    if(healthcheckService.isServiceReady()) {
      return Response.noContent().build();
    } else {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
