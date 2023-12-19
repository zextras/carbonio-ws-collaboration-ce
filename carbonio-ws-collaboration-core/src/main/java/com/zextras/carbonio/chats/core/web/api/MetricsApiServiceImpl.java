// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import com.zextras.carbonio.chats.api.AuthApiService;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.infrastructure.metrics.PrometheusService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.security.AuthenticationMethod;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.TokenDto;
import com.zextras.carbonio.metrics.api.MetricsApiService;
import com.zextras.carbonio.metrics.api.NotFoundException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.util.Optional;

@Singleton
public class MetricsApiServiceImpl implements MetricsApiService {

  private final PrometheusService  prometheusService;

  @Inject
  public MetricsApiServiceImpl(
          PrometheusService prometheusService) {
    this.prometheusService = prometheusService;
  }
  @Override
  public Response metrics(SecurityContext securityContext){
    String responseBody = prometheusService.getRegistry().scrape();

    return Response
        .ok(responseBody)
        .build();
  }
}
