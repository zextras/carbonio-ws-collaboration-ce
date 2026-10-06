// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import com.zextras.carbonio.ws.plugin.api.PluginDescriptor;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/plugins")
@PermitAll
public class PluginResource {

  @Inject PluginRegistry registry;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<PluginDescriptor> list() {
    return registry.loaded();
  }
}
