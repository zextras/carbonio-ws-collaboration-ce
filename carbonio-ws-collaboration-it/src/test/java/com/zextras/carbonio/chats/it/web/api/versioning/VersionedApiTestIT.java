// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api.versioning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vdurmont.semver4j.Semver;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.web.api.versioning.ChangeSet;
import com.zextras.carbonio.chats.core.web.api.versioning.VersionMigrationsRegistry;
import com.zextras.carbonio.chats.core.web.api.versioning.filter.VersionedRequestFilter;
import com.zextras.carbonio.chats.core.web.api.versioning.filter.VersionedResponseFilter;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.web.api.versioning.migration.AddFullNameMigration;
import com.zextras.carbonio.chats.it.web.api.versioning.migration.RemoveEmailMigration;
import com.zextras.carbonio.chats.it.web.api.versioning.migration.RemoveZipCodeMigration;
import com.zextras.carbonio.chats.it.web.api.versioning.migration.RenamePhoneToPhoneNumberMigration;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ApiIntegrationTest
class VersionedApiTestIT {

  public static final String DUMMY_API_BASE_ENDPOINT = "/dummy/versioned/api";
  public static final String DUMMY_API_LIST_ENDPOINT = DUMMY_API_BASE_ENDPOINT + "/list";
  private final DummyVersionedApi dummyVersionedApi;
  private final ResteasyRequestDispatcher defaultDispatcher;
  private final ObjectMapper objectMapper;

  public VersionedApiTestIT(
      DummyVersionedApi dummyVersionedApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper) {
    this.dummyVersionedApi = dummyVersionedApi;
    this.defaultDispatcher = dispatcher;
    this.objectMapper = objectMapper;
  }

  @AfterEach
  void init() {
    VersionMigrationsRegistry.REGISTRY.clear();
  }

  @Test
  void downgrade_migration()
      throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    registerDummyApi(defaultDispatcher);

    ChangeSet changeSet1 =
        new ChangeSet(new Semver("1.5.0"), DummyModel.class, List.of(new AddFullNameMigration()));
    ChangeSet changeSet2 =
        new ChangeSet(
            new Semver("1.4.0"),
            DummyModel.class,
            List.of(new RenamePhoneToPhoneNumberMigration(), new RemoveEmailMigration()));
    VersionMigrationsRegistry versionRegistry = VersionMigrationsRegistry.REGISTRY;
    versionRegistry.register(changeSet1);
    versionRegistry.register(changeSet2);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "1.3.0");
    MockHttpResponse response =
        defaultDispatcher.get(DUMMY_API_BASE_ENDPOINT, null, headerWithVersion);

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
    registerDummyApi(defaultDispatcher);

    ChangeSet matchingChangeSet =
        new ChangeSet(new Semver("1.5.0"), DummyModel.class, List.of(new AddFullNameMigration()));
    ChangeSet wrongChangeSet =
        new ChangeSet(
            new Semver("1.4.0"), ExampleModel.class, List.of(new RemoveZipCodeMigration()));

    VersionMigrationsRegistry versionRegistry = VersionMigrationsRegistry.REGISTRY;
    versionRegistry.register(matchingChangeSet);
    versionRegistry.register(wrongChangeSet);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "1.3.0");
    MockHttpResponse response =
        defaultDispatcher.get(DUMMY_API_BASE_ENDPOINT, null, headerWithVersion);

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
  void migrateDTOList()
      throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    registerDummyApi(defaultDispatcher);

    ChangeSet matchingChangeSet =
        new ChangeSet(new Semver("1.5.0"), DummyModel.class, List.of(new AddFullNameMigration()));
    ChangeSet wrongChangeSet =
        new ChangeSet(
            new Semver("1.4.0"), ExampleModel.class, List.of(new RemoveZipCodeMigration()));

    VersionMigrationsRegistry versionRegistry = VersionMigrationsRegistry.REGISTRY;
    versionRegistry.register(matchingChangeSet);
    versionRegistry.register(wrongChangeSet);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "1.3.0");
    MockHttpResponse response =
        defaultDispatcher.get(DUMMY_API_LIST_ENDPOINT, null, headerWithVersion);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    ObjectNode jon =
        JsonNodeFactory.instance
            .objectNode()
            .put("firstName", "John")
            .put("lastName", "Doe")
            .put("phoneNumber", "+123456789");
    ObjectNode jane =
        JsonNodeFactory.instance
            .objectNode()
            .put("firstName", "Jane")
            .put("lastName", "Doe")
            .put("phoneNumber", "+125457789");
    var expected = new ArrayNode(JsonNodeFactory.instance).add(jon).add(jane);

    assertEquals(expected, objectMapper.readTree(response.getContentAsString()));
  }

  @Test
  void response_header_has_same_version() throws URISyntaxException {
    registerDummyApi(defaultDispatcher);

    var requestVersion = "1.5.0";
    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, requestVersion);
    MockHttpResponse response =
        defaultDispatcher.get(DUMMY_API_BASE_ENDPOINT, null, headerWithVersion);

    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    var responseVersion =
        response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER).toString();
    assertEquals(requestVersion, responseVersion);
  }

  @Test
  void downgrade_to_oldest_version_without_version_header()
      throws URISyntaxException, UnsupportedEncodingException, JsonProcessingException {
    var oldestVersion = new Semver("1.5.0");
    var middleVersion = oldestVersion.nextMinor();
    var newestVersion = oldestVersion.nextMinor().nextPatch();

    var dispatcher =
        buildRequestDispatcherWith(
            new VersionedResponseFilterTest("ignore", oldestVersion.getValue()));
    registerDummyApi(dispatcher);

    ChangeSet changeSet1 =
        new ChangeSet(
            new Semver(newestVersion.getValue()),
            DummyModel.class,
            List.of(new AddFullNameMigration()));
    ChangeSet changeSet2 =
        new ChangeSet(
            new Semver(middleVersion.getValue()),
            DummyModel.class,
            List.of(new RenamePhoneToPhoneNumberMigration(), new RemoveEmailMigration()));
    VersionMigrationsRegistry versionRegistry = VersionMigrationsRegistry.REGISTRY;
    versionRegistry.register(changeSet1);
    versionRegistry.register(changeSet2);

    MockHttpResponse response = dispatcher.get(DUMMY_API_BASE_ENDPOINT, null);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    var expectedOldestModel =
        JsonNodeFactory.instance
            .objectNode()
            .put("firstName", "John")
            .put("lastName", "Doe")
            .put("phone", "+123456789")
            .put("email", "john.doe@example.com");
    assertEquals(expectedOldestModel, objectMapper.readTree(response.getContentAsString()));
  }

  @Test
  void reject_request_with_newer_version() throws URISyntaxException {
    String currentVersion = "1.5.0";
    var dispatcher =
        buildRequestDispatcherWith(
            new VersionedRequestFilterTest(currentVersion),
            new VersionedResponseFilterTest(currentVersion, "ignore"));
    registerDummyApi(dispatcher);

    Semver newerVersion = new Semver("1.6.0");
    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, newerVersion.getValue());

    MockHttpResponse response = dispatcher.get(DUMMY_API_BASE_ENDPOINT, null, headerWithVersion);

    assertEquals(422, response.getStatus());
    assertEquals(
        currentVersion, response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER));
  }

  @Test
  void semantic_version_exception() throws URISyntaxException {
    registerDummyApi(defaultDispatcher);

    var headerWithVersion = Map.of(ChatsConstant.API_VERSION_HEADER, "not a semantic version");
    MockHttpResponse response =
        defaultDispatcher.get(DUMMY_API_BASE_ENDPOINT, null, headerWithVersion);

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(
        VersionProvider.getVersion(),
        response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER));
  }

  private static Stream<Arguments> HeaderVersionScenarios() {
    return Stream.of(
        Arguments.of(new AlignedVersions()),
        Arguments.of(new ClientWithOlderVersion()),
        Arguments.of(new ClientWithNewerVersion()),
        Arguments.of(new ClientWithNoHeaderVersion()));
  }

  @ParameterizedTest
  @MethodSource("HeaderVersionScenarios")
  void headerVersionScenarios(HeaderVersionScenario scenario) throws URISyntaxException {
    var server = scenario.serverStatus();
    var client = scenario.clientStatus();
    var dispatcher =
        buildRequestDispatcherWith(
            new VersionedRequestFilterTest(server.currentVersion),
            new VersionedResponseFilterTest(server.currentVersion, server.oldestVersion));
    registerDummyApi(dispatcher);

    var headerWithVersion =
        client.versionRequest == null
            ? Map.of("NO_VERSION_HEADER", "") // When a client is too old to send the version header
            : Map.of(ChatsConstant.API_VERSION_HEADER, client.versionRequest);
    MockHttpResponse response = dispatcher.get(DUMMY_API_BASE_ENDPOINT, null, headerWithVersion);

    assertEquals(
        scenario.expectedResponseVersion(),
        response.getOutputHeaders().getFirst(ChatsConstant.API_VERSION_HEADER));
  }

  /****************************************
   * Utility Functions
   ****************************************/

  private void registerDummyApi(ResteasyRequestDispatcher dispatcher) {
    dispatcher.getRegistry().addSingletonResource(dummyVersionedApi);
  }

  private ResteasyRequestDispatcher buildRequestDispatcherWith(
      VersionedResponseFilter customVersionFilter) {
    Dispatcher d = MockDispatcherFactory.createDispatcher();
    d.getProviderFactory().register(customVersionFilter);
    return new ResteasyRequestDispatcher(d);
  }

  private ResteasyRequestDispatcher buildRequestDispatcherWith(
      VersionedRequestFilter customRequestFilter, VersionedResponseFilter customResponseFilter) {
    Dispatcher d = MockDispatcherFactory.createDispatcher();
    d.getProviderFactory().register(customRequestFilter);
    d.getProviderFactory().register(customResponseFilter);
    return new ResteasyRequestDispatcher(d);
  }

  /****************************************
   * Object Helpers
   ****************************************/

  class VersionedResponseFilterTest extends VersionedResponseFilter {
    private final String currentVersion;
    private final String oldestVersion;

    public VersionedResponseFilterTest(String currentVersion, String oldestVersion) {
      super();
      this.currentVersion = currentVersion;
      this.oldestVersion = oldestVersion;
    }

    @Override
    public String getCurrentVersion() {
      return this.currentVersion;
    }

    @Override
    public String getOldestVersion() {
      return oldestVersion;
    }
  }

  class VersionedRequestFilterTest extends VersionedRequestFilter {
    private final String currentVersion;

    public VersionedRequestFilterTest(String currentVersion) {
      super();
      this.currentVersion = currentVersion;
    }

    @Override
    public String getCurrentVersion() {
      return this.currentVersion;
    }
  }

  /****************************************
   * Header Version Scenarios
   ****************************************/

  private interface HeaderVersionScenario {
    ServerStatus serverStatus();

    ClientStatus clientStatus();

    String expectedResponseVersion();
  }

  private record AlignedVersions(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements HeaderVersionScenario {
    public AlignedVersions() {
      this(new ServerStatus("1.6.0"), new ClientStatus("1.6.0"), "1.6.0");
    }
  }

  private record ClientWithOlderVersion(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements HeaderVersionScenario {
    public ClientWithOlderVersion() {
      this(new ServerStatus("1.6.0"), new ClientStatus("1.5.12"), "1.5.12");
    }
  }

  private record ClientWithNewerVersion(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements HeaderVersionScenario {
    public ClientWithNewerVersion() {
      this(new ServerStatus("1.6.0"), new ClientStatus("1.7.0"), "1.6.0");
    }
  }

  private record ClientWithNoHeaderVersion(
      ServerStatus serverStatus, ClientStatus clientStatus, String expectedResponseVersion)
      implements HeaderVersionScenario {
    public ClientWithNoHeaderVersion() {
      this(new ServerStatus("1.6.0", "1.4.0"), new ClientStatus(null), "1.4.0");
    }
  }

  private record ServerStatus(String currentVersion, String oldestVersion) {
    public ServerStatus(String currentVersion) {
      this(currentVersion, "ignore");
    }
  }

  private record ClientStatus(String versionRequest) {}
}
