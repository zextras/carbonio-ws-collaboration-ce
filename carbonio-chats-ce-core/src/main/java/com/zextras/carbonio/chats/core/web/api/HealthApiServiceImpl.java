// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.HealthApiService;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

public class HealthApiServiceImpl implements HealthApiService {

  private final int INTERNAL_SERVER_ERROR_STATUS_CODE = 500;
  private final int DEPENDENCY_EXCEPTION_STATUS_CODE = 424;
  private final int NO_CONTENT_STATUS_CODE = 204;
  private final HealthcheckService healthcheckService;

  @Inject
  public HealthApiServiceImpl(HealthcheckService healthcheckService) {
    this.healthcheckService = healthcheckService;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.TRACE)
  public Response getHealthStatus(SecurityContext securityContext) {
    return Response.ok()
      .entity(healthcheckService.getServiceHealth())
      .build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.TRACE)
  public Response isLive(SecurityContext securityContext) {
    return Response.noContent().build();
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.TRACE)
  public Response isReady(SecurityContext securityContext) {
    int status;
    switch (healthcheckService.getServiceStatus()) {
      case ERROR:
        status = INTERNAL_SERVER_ERROR_STATUS_CODE;
        break;
      case WARN:
        status = DEPENDENCY_EXCEPTION_STATUS_CODE;
        break;
      default:
        status = NO_CONTENT_STATUS_CODE;
    }
    return Response.status(status).build();
  }
}
