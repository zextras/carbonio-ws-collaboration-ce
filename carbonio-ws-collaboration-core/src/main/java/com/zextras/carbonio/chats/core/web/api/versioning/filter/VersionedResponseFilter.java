// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api.versioning.filter;

import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.api.versioning.VersionMigrationsRegistry;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;

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
          .add(ChatsConstant.API_VERSION_HEADER, VersionProvider.getVersion());
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

    Class<?> modelClass = retrieveModelClassFrom(responseContext);
    if (modelClass == null) {
      ChatsLogger.warn(
          String.format("Could not determine model class for response entity: %s", entity));
      return;
    }
    var apiVersionMigrator = registry.migratorFor(apiVersion, modelClass);
    var transformed = apiVersionMigrator.downgrade(entity);
    responseContext.setEntity(transformed);
  }

  /**
   * Naive retrieval implementation for Response entities that contain Lists.
   *
   * <p>Due to Java's type erasure, the DTO type in List<DTO> is not available at runtime, making it
   * impossible to determine the actual element type through standard reflection. To work around
   * this limitation, this implementation uses reflection combined with Swagger annotations to
   * extract the DTO class type for HTTP 200 responses.
   *
   * <p>Note: Both the service classes and their annotations are automatically generated from the
   * OpenAPI specification file.
   */
  public static Class<?> retrieveModelClassFrom(ContainerResponseContext responseContext) {
    Annotation[] annotations = responseContext.getEntityAnnotations();

    for (Annotation annotation : annotations) {
      if (annotation instanceof ApiResponses apiResponses) {
        ApiResponse[] responses = apiResponses.value();
        for (ApiResponse response : responses) {
          if (response.code() == 200) {
            return response.response();
          }
        }
        return null;
      }
    }
    return null;
  }
}
