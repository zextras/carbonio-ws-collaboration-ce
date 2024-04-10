// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.HealthApiService;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;

public class HealthApiServiceImpl implements HealthApiService {

  private static final int MANDATARY_SERVICE_ERROR_CODE = 500;
  private static final int OPTIONAL_SERVICE_ERROR_CODE = 204;

  private final HealthcheckService healthcheckService;

  @Inject
  public HealthApiServiceImpl(HealthcheckService healthcheckService) {
    this.healthcheckService = healthcheckService;
  }

  @Override
  @TimedCall(logLevel = ChatsLoggerLevel.TRACE)
  public Response getHealthStatus(SecurityContext securityContext) {
    return Response.ok().entity(healthcheckService.getServiceHealth()).build();
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
        status = MANDATARY_SERVICE_ERROR_CODE;
        break;
      case WARN:
        status = OPTIONAL_SERVICE_ERROR_CODE;
        break;
      default:
        status = Status.NO_CONTENT.getStatusCode();
    }
    return Response.status(status).build();
  }
}
