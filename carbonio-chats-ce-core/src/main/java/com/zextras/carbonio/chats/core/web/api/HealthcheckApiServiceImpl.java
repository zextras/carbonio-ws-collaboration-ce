package com.zextras.carbonio.chats.core.web.api;


import com.zextras.carbonio.chats.core.api.HealthcheckApiService;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class HealthcheckApiServiceImpl implements HealthcheckApiService {

  private final HealthcheckService healthcheckService;

  @Inject
  public HealthcheckApiServiceImpl(HealthcheckService healthcheckService) {
    this.healthcheckService = healthcheckService;
  }

  @Override
  public Response healthcheck(SecurityContext securityContext) {
    healthcheckService.healthcheck(securityContext);
    return Response.ok().build();
  }
}
