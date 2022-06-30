package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.SupportedApiService;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.provider.AppInfoProvider;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class SupportedApiServiceImpl implements SupportedApiService {

  private final AppInfoProvider appInfoProvider;

  @Inject
  public SupportedApiServiceImpl(AppInfoProvider appInfoProvider) {
    this.appInfoProvider = appInfoProvider;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.TRACE)
  public Response getSupportedVersions(SecurityContext securityContext) {
    Optional<String> version = appInfoProvider.getVersion();
    if (version.isPresent()) {
      return Response.status(Status.OK).entity(List.of(version.get())).build();
    } else {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
