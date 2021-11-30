package com.zextras.chats.core.api;


import java.util.UUID;
import com.zextras.chats.core.model.UserDto;
import java.util.List;
import javax.annotation.Generated;
import javax.ws.rs.core.SecurityContext;

@Generated(value = "org.openapitools.codegen.languages.JavaResteasyServerCodegen")
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
