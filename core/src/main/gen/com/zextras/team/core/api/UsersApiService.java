package com.zextras.team.core.api;


import java.util.UUID;
import com.zextras.team.core.model.UserDto;
import java.util.List;
import javax.annotation.Generated;
import javax.ws.rs.core.SecurityContext;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen", date = "2021-11-22T10:48:21.557692+01:00[Europe/Rome]")
public interface UsersApiService {

  /**
   * Retrieves a user
   *
   * @param userId user identifier {@link UUID }
   * @param securityContext security context {@link SecurityContext}
   * @return Requested user {@link UserDto }
  **/
  UserDto getUserById(UUID userId, SecurityContext securityContext);

}
