package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.ConsulApiService;
import com.zextras.carbonio.chats.core.service.CosulService;
import com.zextras.carbonio.chats.model.ConsulPropertyDto;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class ConsulApiServiceImpl implements ConsulApiService {

  private final CosulService cosulService;

  @Inject
  public ConsulApiServiceImpl(CosulService cosulService) {
    this.cosulService = cosulService;
  }

  @Override
  public Response setConsulProperties(
    List<ConsulPropertyDto> consulPropertyDto, SecurityContext securityContext
  ) {
    cosulService.setConsulProperties(consulPropertyDto);
    return Response.status(Status.NO_CONTENT).build();
  }
}
