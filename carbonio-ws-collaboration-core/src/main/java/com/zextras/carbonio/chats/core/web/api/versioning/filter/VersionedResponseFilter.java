// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning.filter;

import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.web.api.versioning.VersionMigrationsRegistry;
import com.zextras.carbonio.chats.openapi.versioning.OpenApiVersionProvider;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class VersionedResponseFilter implements ContainerResponseFilter {

  private final VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    if (responseContext.getStatus() < 200 || responseContext.getStatus() >= 300) {
      responseContext
          .getHeaders()
          .add(ChatsConstant.API_VERSION_HEADER, OpenApiVersionProvider.getVersion());
      return;
    }

    // api version header correctness is checked in VersionedRequestFilter
    String headerString = requestContext.getHeaderString(ChatsConstant.API_VERSION_HEADER);
    if (headerString == null) return;
    var apiVersion = new Semver(headerString);

    responseContext.getHeaders().add(ChatsConstant.API_VERSION_HEADER, apiVersion.getValue());

    Object entity = responseContext.getEntity();
    if (entity == null) return;

    if (!registry.hasMigrationsAfter(apiVersion)) return;

    var apiVersionMigrator = registry.migratorFor(apiVersion, entity.getClass());
    var transformed = apiVersionMigrator.downgrade(entity);
    responseContext.setEntity(transformed);
  }
}
