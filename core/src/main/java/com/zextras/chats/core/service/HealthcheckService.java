package com.zextras.chats.core.service;

import javax.ws.rs.core.SecurityContext;

public interface HealthcheckService {

  /**
   * healthcheck endpoint which will answer according to the service state
   *
   * @param securityContext security context {@link SecurityContext}
   **/
  void healthcheck(SecurityContext securityContext);
}
