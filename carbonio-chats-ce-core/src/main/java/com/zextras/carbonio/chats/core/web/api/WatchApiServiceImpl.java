package com.zextras.carbonio.chats.core.web.api;

import com.ecwid.consul.v1.kv.model.GetValue;
import com.zextras.carbonio.chats.api.WatchApiService;
import com.zextras.carbonio.chats.core.service.WatchService;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class WatchApiServiceImpl implements WatchApiService {

  private final WatchService watchService;

  @Inject
  public WatchApiServiceImpl(WatchService watchService) {
    this.watchService = watchService;
  }

  @Override
  public Response setConsulProperties(
    List<GetValue> consulPropertyDto, SecurityContext securityContext
  ) {
    watchService.setConsulProperties(consulPropertyDto);
    return Response.status(Status.NO_CONTENT).build();

  }
}
