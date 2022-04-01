package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.model.UserDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
public class UsersApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final AppClock                     clock;

  public UsersApiIT(
    ResteasyRequestDispatcher dispatcher,
    ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    Clock clock
  ) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.clock = (AppClock) clock;
  }

  @Nested
  @DisplayName("Get user tests")
  class GetUserTests {

    private String url(UUID userId) {
      return String.format("/users/%s", userId);
    }

    @Test
    @DisplayName("Returns the requested user")
    public void getUser_testOK() throws Exception {
      UUID userId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
      clock.fixTimeAt(OffsetDateTime.now().toInstant());
      integrationTestUtils.generateAndSaveUser(userId, "hello",
        OffsetDateTime.ofInstant(clock.instant(), clock.getZone()), "123");

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userId), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      assertEquals(200, mockHttpResponse.getStatus());
      UserDto user = objectMapper.readValue(mockHttpResponse.getContentAsString(), new TypeReference<>() {
      });
      assertEquals("332a9527-3388-4207-be77-6d7e2978a723", user.getId().toString());
      assertEquals("snoopy@peanuts.com", user.getEmail());
      assertEquals("Snoopy", user.getName());
      assertEquals("hello", user.getStatusMessage());
      assertEquals(clock.instant().getEpochSecond(), user.getLastSeen());
    }

    @Test
    @DisplayName("Returns the requested user")
    public void getUser_testUserNotFound() throws Exception {
      UUID userId = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a722");
      clock.fixTimeAt(OffsetDateTime.now().toInstant());

      MockHttpResponse mockHttpResponse = dispatcher.get(url(userId), "6g2R31FDn9epUpbyLhZSltqACqd33K9qa0b3lsJL");
      assertEquals(404, mockHttpResponse.getStatus());
    }

  }

}
