// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.web.api.versioning.ChangeSet;
import com.zextras.carbonio.chats.core.web.api.versioning.VersionMigrationsRegistry;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.openapi.versioning.OpenApiVersionProvider;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class VersioningApiTestIT {

  private final DummyVersionedApi dummyVersionedApi;
  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;

  public VersioningApiTestIT(
      DummyVersionedApi dummyVersionedApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper) {
    this.dummyVersionedApi = dummyVersionedApi;
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
  }

  @BeforeEach
  public void init() {
    VersionMigrationsRegistry.REGISTRY.clear();
  }

  @Test
  void downgrade_migration()
      throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);
    VersionMigrationsRegistry versionRegistry = VersionMigrationsRegistry.REGISTRY;

    ChangeSet changeSet1 =
        new ChangeSet(new Semver("1.5.0"), DummyModel.class, List.of(new AddFullNameMigration()));
    ChangeSet changeSet2 =
        new ChangeSet(
            new Semver("1.4.0"),
            DummyModel.class,
            List.of(new RenamePhoneToPhoneNumberMigration(), new RemoveEmailMigration()));
    versionRegistry.register(changeSet1);
    versionRegistry.register(changeSet2);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "1.3.0");
    MockHttpResponse response = dispatcher.get("/dummy/versioned/api", null, headerWithVersion);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    var expected =
        JsonNodeFactory.instance
            .objectNode()
            .put("firstName", "John")
            .put("lastName", "Doe")
            .put("phone", "+123456789")
            .put("email", "john.doe@example.com");
    assertEquals(expected, objectMapper.readTree(response.getContentAsString()));
  }

  @Test
  void apply_migrations_for_matching_model_class()
      throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);
    VersionMigrationsRegistry versionRegistry = VersionMigrationsRegistry.REGISTRY;

    ChangeSet matchingChangeSet =
        new ChangeSet(new Semver("1.5.0"), DummyModel.class, List.of(new AddFullNameMigration()));
    ChangeSet wrongChangeSet =
        new ChangeSet(new Semver("1.4.0"), ExampleModel.class, List.of(new RemoveZipCode()));
    versionRegistry.register(matchingChangeSet);
    versionRegistry.register(wrongChangeSet);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "1.3.0");
    MockHttpResponse response = dispatcher.get("/dummy/versioned/api", null, headerWithVersion);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    var expected =
        JsonNodeFactory.instance
            .objectNode()
            .put("firstName", "John")
            .put("lastName", "Doe")
            .put("phoneNumber", "+123456789");
    assertEquals(expected, objectMapper.readTree(response.getContentAsString()));
  }

  @Test
  void response_header_has_same_version() throws URISyntaxException {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);

    var requestVersion = "1.5.0";
    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, requestVersion);
    MockHttpResponse response = dispatcher.get("/dummy/versioned/api", null, headerWithVersion);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    var responseVersion =
        response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER).toString();
    assertEquals(requestVersion, responseVersion);
  }

  @Test
  void latest_version()
      throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);

    MockHttpResponse response = dispatcher.get("/dummy/versioned/api", null);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    var latestModel = new DummyModel("John Doe", "+123456789");
    assertEquals(
        latestModel, objectMapper.readValue(response.getContentAsString(), DummyModel.class));
  }

  @Test
  void newer_version() throws URISyntaxException {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);

    var headerWithVersion =
        Map.of(
            ChatsConstant.API_VERSION_HEADER,
            new Semver(OpenApiVersionProvider.getVersion()).nextMinor().toString());
    MockHttpResponse response = dispatcher.get("/dummy/versioned/api", null, headerWithVersion);

    assertEquals(422, response.getStatus());
    assertEquals(
        OpenApiVersionProvider.getVersion(),
        response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER));
  }

  @Test
  void semantic_version_exception() throws URISyntaxException {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "not a semantic version");
    MockHttpResponse response = dispatcher.get("/dummy/versioned/api", null, headerWithVersion);

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(
        OpenApiVersionProvider.getVersion(),
        response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER));
  }
}
