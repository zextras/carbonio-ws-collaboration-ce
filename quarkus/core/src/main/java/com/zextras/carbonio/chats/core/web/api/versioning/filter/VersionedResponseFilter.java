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
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class VersionedResponseFilter implements ContainerResponseFilter {

  private final VersionMigrationsRegistry registry = VersionMigrationsRegistry.REGISTRY;

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {

    if (responseContext.getStatus() < Status.OK.getStatusCode()
        || responseContext.getStatus() >= Status.MULTIPLE_CHOICES.getStatusCode()) {
      responseContext.getHeaders().add(ChatsConstant.API_VERSION_HEADER, getCurrentVersion());
      return;
    }

    // Only JSON responses are versioned
    if (isNotJsonType(responseContext)) {
      return;
    }

    // api version header correctness is checked in VersionedRequestFilter
    String headerString = requestContext.getHeaderString(ChatsConstant.API_VERSION_HEADER);

    var apiVersion = new Semver(headerString != null ? headerString : getOldestVersion());

    responseContext.getHeaders().add(ChatsConstant.API_VERSION_HEADER, apiVersion.getValue());
    if (responseContext.getMediaType() == null) {
      // The CE UI compares the content type strictly: no charset parameter.
      responseContext.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);
    }

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
    if (apiVersionMigrator.getMigrations().isEmpty()) return;

    var transformed = apiVersionMigrator.downgrade(entity);
    responseContext.setEntity(transformed);
  }

  public String getCurrentVersion() {
    return VersionProvider.getVersion();
  }

  public String getOldestVersion() {
    List<String> supportedVersions = VersionProvider.getSupportedVersions();
    return supportedVersions.get(supportedVersions.size() - 1);
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
          if (response.code() == Status.OK.getStatusCode()
              || response.code() == Status.CREATED.getStatusCode()) {
            return response.response();
          }
        }
        return null;
      }
    }
    return null;
  }

  private boolean isNotJsonType(ContainerResponseContext responseContext) {
    MediaType contentType = mediaTypeOf(responseContext);
    return contentType == null || !contentType.isCompatible(MediaType.APPLICATION_JSON_TYPE);
  }

  /**
   * The response media type. Quarkus REST negotiates the content type of a plain entity only when
   * serialising it, after the response filters, so a response without an explicit type falls back
   * to the {@code @Produces} of the resource method (the generated APIs declare it), which is what
   * RESTEasy classic exposed here.
   */
  static MediaType mediaTypeOf(ContainerResponseContext responseContext) {
    MediaType contentType = responseContext.getMediaType();
    if (contentType != null || responseContext.getEntityAnnotations() == null) {
      return contentType;
    }
    for (Annotation annotation : responseContext.getEntityAnnotations()) {
      if (annotation instanceof Produces produces && produces.value().length > 0) {
        return MediaType.valueOf(produces.value()[0].split(",")[0].trim());
      }
    }
    return null;
  }
}
