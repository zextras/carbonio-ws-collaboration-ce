package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;

public interface CapabilityService {

  CapabilitiesDto getCapabilities(UserPrincipal currentUser);

}
