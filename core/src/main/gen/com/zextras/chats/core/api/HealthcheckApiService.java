package com.zextras.chats.core.api;


import java.util.List;
import javax.annotation.Generated;
import javax.ws.rs.core.SecurityContext;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
public interface HealthcheckApiService {

  /**
   * healthcheck endpoint which will answer according to the service state
   *
   * @param securityContext security context {@link SecurityContext}
  **/
  void healthcheck(SecurityContext securityContext);

}
