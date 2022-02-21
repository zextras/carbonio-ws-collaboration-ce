package com.zextras.carbonio.chats.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.Utils.MockedAccount;
import com.zextras.carbonio.chats.it.Utils.MockedFiles;
import com.zextras.carbonio.chats.it.Utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.annotations.IntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.UserManagementMockServer;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class RoomsApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper              objectMapper;
  private final IntegrationTestUtils      integrationTestUtils;
  private final UserManagementMockServer  userManagementMockServer;
  private final AppClock                  clock;

  public RoomsApiIT(
    RoomsApi roomsApi, ResteasyRequestDispatcher dispatcher, ObjectMapper objectMapper,
    IntegrationTestUtils integrationTestUtils,
    UserManagementMockServer userManagementMockServer, Clock clock
  ) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.userManagementMockServer = userManagementMockServer;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.clock = (AppClock) clock;
  }

  private static UUID   user1Id;
  private static UUID   user2Id;
  private static UUID   user3Id;
  private static String user1Token;
  private static String user3Token;

  @BeforeAll
  public static void initAll() {
    int i = new Random().nextInt(Integer.MAX_VALUE);
    user1Id = MockedAccount.getAccount(i).getUUID();
    user1Token = MockedAccount.getAccount(i).getToken();
    user2Id = MockedAccount.getAccount(i + 1).getUUID();
    user3Id = MockedAccount.getAccount(i + 2).getUUID();
    user3Token = MockedAccount.getAccount(i + 2).getToken();
  }

  @Test
  public void listRoom_test() throws Exception {
    UUID room1Id = UUID.randomUUID();
    UUID room2Id = UUID.randomUUID();

    integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
    integrationTestUtils.generateAndSaveRoom(room2Id, RoomTypeDto.GROUP, "room2", List.of(user1Id, user2Id));

    MockHttpResponse response = dispatcher.get("/rooms", user1Token);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    List<RoomDto> rooms = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
    });
    assertEquals(2, rooms.size());
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
  }

  @Test
  @DisplayName("Given creation fields, if there is current user into invites list throws a 'bad request' exception")
  public void insertRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser() throws Exception {
    MockHttpResponse response = dispatcher.post("/rooms",
      getInsertRoomRequestBody("room", "Room", RoomTypeDto.GROUP, List.of(user1Id, user2Id, user3Id)),
      null, user1Token);

    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals(0, response.getOutput().length);
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
  }

  private String getInsertRoomRequestBody(
    String name, @Nullable String description, RoomTypeDto typeDto, @Nullable List<UUID> membersIds
  ) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append(String.format("\"name\": \"%s\",", name));
    Optional.ofNullable(description).ifPresent(desc -> sb.append(String.format("\"description\": \"%s\",", desc)));
    sb.append(String.format("\"type\": \"%s\",", typeDto));
    Optional.ofNullable(membersIds).ifPresent(ids -> {
      sb.append("[");
      ids.forEach(id -> sb.append(String.format("\"%s\",", id)));
      sb.append("]");
    });
    sb.append("}");
    return sb.toString();
  }

  @Test
  public void getRoomPicture_test() throws URISyntaxException {
    FileMock fileMock = MockedFiles.getRandomImage();
    UUID roomId = UUID.fromString(fileMock.getId());
    integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));
    integrationTestUtils.generateAndSaveFileMetadata(fileMock, FileMetadataType.ROOM_AVATAR, user1Id, roomId);

    MockHttpResponse response = dispatcher.get(String.format("/rooms/%s/picture", roomId), user1Token);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
  }

  @Nested
  @DisplayName("Gets list of room attachment information tests")
  public class ListOfRoomAttachmentInformationTests {

    private static final String FORMAT_URL = "/rooms/%s/attachments";

    private String formatUrl(UUID roomId) {
      return String.format(FORMAT_URL, roomId);
    }

    @Test
    @DisplayName("Given a room identifier, correctly returns all attachments info of the required room")
    public void listRoomAttachmentInfo_testOk() throws Exception {

      UUID room1Id = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(room1Id, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id, user3Id));

      clock.fixTimeAt(Instant.parse("2022-01-01T00:00:00Z"));
      FileMetadata file1 = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2020-12-31T00:00:00Z"));
      FileMetadata file2 = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.fixTimeAt(Instant.parse("2022-01-02T00:00:00Z"));
      FileMetadata file3 = integrationTestUtils.generateAndSaveFileMetadata(UUID.randomUUID(),
        FileMetadataType.ATTACHMENT, user1Id, room1Id);
      clock.removeFixTime();

      MockHttpResponse response = dispatcher.get(formatUrl(room1Id), user1Token);

      assertEquals(200, response.getStatus());
      List<AttachmentDto> attachments = objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {
      });
      assertEquals(3, attachments.size());
      assertEquals(
        List.of(file3.getId(), file1.getId(), file2.getId()),
        attachments.stream()
          .map(attachment -> attachment.getId().toString())
          .collect(Collectors.toList()));
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user1Token), 1);
    }

    @Test
    @DisplayName("Given a room identifier, if the user is not authenticated return a status code 401")
    public void listRoomAttachmentInfo_testErrorUnauthenticatedUser() throws Exception {
      MockHttpResponse response = dispatcher.get(formatUrl(UUID.randomUUID()), null);

      assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
    }

    @Test
    @DisplayName("Given a room identifier, if authenticated user isn't a room member then return a status code 403")
    public void listRoomAttachmentInfo_testErrorUserIsNotARoomMember() throws Exception {
      UUID roomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(roomId, RoomTypeDto.GROUP, "room1", List.of(user1Id, user2Id));

      MockHttpResponse response = dispatcher.get(formatUrl(roomId), user3Token);
      assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());
      assertEquals(0, response.getOutput().length);
      userManagementMockServer.verify("GET", String.format("/auth/token/%s", user3Token), 1);
    }

  }
}