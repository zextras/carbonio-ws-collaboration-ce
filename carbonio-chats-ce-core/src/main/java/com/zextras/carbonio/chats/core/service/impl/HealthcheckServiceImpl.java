package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.service.HealthcheckService;
import javax.ws.rs.core.SecurityContext;

public class HealthcheckServiceImpl implements HealthcheckService {

  @Override
  public void healthcheck(SecurityContext securityContext) {
    // TODO: 05/01/22  no healthchecks as of right now
  }
}
