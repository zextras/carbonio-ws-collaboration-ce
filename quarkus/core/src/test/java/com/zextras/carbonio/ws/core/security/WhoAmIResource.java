// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import com.zextras.carbonio.ws.plugin.api.CurrentUser;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.Map;

@Path("/test/whoami")
@Authenticated
public class WhoAmIResource {

  @Inject CurrentUser currentUser;

  @GET
  public Map<String, Object> whoami() {
    UserPrincipal u = currentUser.get();
    return Map.of(
        "id", u.id().toString(),
        "type", u.type().name(),
        "queueId", u.queueId() == null ? "" : u.queueId().toString());
  }
}
