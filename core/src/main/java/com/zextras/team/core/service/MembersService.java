package com.zextras.team.core.service;

import java.util.UUID;
import javax.ws.rs.core.SecurityContext;

public interface MembersService {

  void modifyOwner(UUID roomId, UUID userId, boolean isOwner, SecurityContext securityContext);


}
