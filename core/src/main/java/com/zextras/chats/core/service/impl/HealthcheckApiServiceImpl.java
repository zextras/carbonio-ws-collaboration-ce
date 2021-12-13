package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.api.HealthcheckApiService;
import javax.ws.rs.core.SecurityContext;

public class HealthcheckApiServiceImpl implements HealthcheckApiService {

  @Override
  public void healthcheck(SecurityContext securityContext) {
    // no healthchecks as of right now
  }
}
