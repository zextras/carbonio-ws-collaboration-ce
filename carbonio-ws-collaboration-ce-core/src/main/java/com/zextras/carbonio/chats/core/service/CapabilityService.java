// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;

public interface CapabilityService {

  CapabilitiesDto getCapabilities(UserPrincipal currentUser);

}
