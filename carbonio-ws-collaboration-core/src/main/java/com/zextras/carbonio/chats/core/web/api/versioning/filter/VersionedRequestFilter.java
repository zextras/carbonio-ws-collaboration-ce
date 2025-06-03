// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning.filter;

import com.vdurmont.semver4j.Semver;
import com.vdurmont.semver4j.SemverException;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.openapi.versioning.OpenApiVersionProvider;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class VersionedRequestFilter implements ContainerRequestFilter {

  private static final int UNPROCESSABLE_ENTITY_STATUS = 422;

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {

    String headerString = containerRequestContext.getHeaderString(ChatsConstant.API_VERSION_HEADER);
    if (headerString == null) return;

    Semver apiVersion;
    try {
      apiVersion = new Semver(headerString);
    } catch (SemverException e) {
      containerRequestContext.abortWith(
          Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build());
      return;
    }

    if (apiVersion.isGreaterThan(OpenApiVersionProvider.getVersion())) {
      containerRequestContext.abortWith(Response.status(UNPROCESSABLE_ENTITY_STATUS).build());
    }
  }
}
