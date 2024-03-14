// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.event.UserPictureChanged;
import com.zextras.carbonio.chats.core.data.event.UserPictureDeleted;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.UserProfile;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.profiling.ProfilingService;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.UserRepository;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.UserDto;
import jakarta.ws.rs.core.Response.Status;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserServiceImplTest {

  private final UserService userService;
  private final ProfilingService profilingService;
  private final UserRepository userRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService storagesService;
  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher eventDispatcher;
  private final AppConfig appConfig;
  private final Clock clock;

  public UserServiceImplTest() {
    this.profilingService = mock(ProfilingService.class);
    this.userRepository = mock(UserRepository.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.appConfig = mock(AppConfig.class);
    this.clock = mock(Clock.class);
    this.userService =
        new UserServiceImpl(
            profilingService,
            userRepository,
            fileMetadataRepository,
            storagesService,
            subscriptionRepository,
            eventDispatcher,
            appConfig,
            clock);
  }

  @BeforeEach
  public void init() {
    when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());
  }

  @Nested
  @DisplayName("Get user by id tests")
  class GetUserByIdTests {

    @Test
    @DisplayName("Returns the user with every info")
    public void getUserById_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString()))
          .thenReturn(
              Optional.of(
                  User.create()
                      .id(requestedUserId.toString())
                      .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
                      .statusMessage("my status!")));
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(
              Optional.of(
                  UserProfile.create(requestedUserId)
                      .email("test@example.com")
                      .domain("mydomain.com")
                      .name("test user")));

      UserDto userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), userById.getPictureUpdatedAt());
      assertEquals("my status!", userById.getStatusMessage());
      assertEquals("test user", userById.getName());
      assertEquals("test@example.com", userById.getEmail());
    }

    @Test
    @DisplayName("Returns the user with the profiling info if it's not found in out db")
    public void getUserById_testDbNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(
              Optional.of(
                  UserProfile.create(requestedUserId)
                      .email("test@example.com")
                      .domain("mydomain.com")
                      .name("test user")));

      UserDto userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertNull(userById.getPictureUpdatedAt());
      assertNull(userById.getStatusMessage());
      assertEquals("test user", userById.getName());
      assertEquals("test@example.com", userById.getEmail());
    }

    @Test
    @DisplayName("Throws NotFoundException if the user was not found using the profiling service")
    public void getUserById_testProfilingNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(Optional.empty());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> userService.getUserById(requestedUserId, currentPrincipal));
    }
  }

  @Nested
  @DisplayName("Get users by ids tests")
  class GetUsersByIdsTests {

    @Test
    @DisplayName("Returns all the users with every info")
    public void getUsersByIds_testOk() {
      UUID requestedUserId1 = UUID.randomUUID();
      UUID requestedUserId2 = UUID.randomUUID();
      UUID requestedUserId3 = UUID.randomUUID();
      List<String> requestedUserIds =
          Arrays.asList(
              requestedUserId1.toString(),
              requestedUserId2.toString(),
              requestedUserId3.toString());

      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId1);

      when(userRepository.getByIds(requestedUserIds))
          .thenReturn(
              Arrays.asList(
                  User.create()
                      .id(requestedUserId1.toString())
                      .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
                      .statusMessage("my status 1"),
                  User.create()
                      .id(requestedUserId2.toString())
                      .pictureUpdatedAt(OffsetDateTime.parse("2022-02-02T00:00:00Z"))
                      .statusMessage("my status 2"),
                  User.create()
                      .id(requestedUserId3.toString())
                      .pictureUpdatedAt(OffsetDateTime.parse("2022-03-03T00:00:00Z"))
                      .statusMessage("my status 3")));
      when(profilingService.getByIds(currentPrincipal, requestedUserIds))
          .thenReturn(
              Arrays.asList(
                  UserProfile.create(requestedUserId1)
                      .email("test1@example.com")
                      .domain("mydomain.com")
                      .name("test user 1"),
                  UserProfile.create(requestedUserId2)
                      .email("test2@example.com")
                      .domain("mydomain.com")
                      .name("test user 2"),
                  UserProfile.create(requestedUserId3)
                      .email("test3@example.com")
                      .domain("mydomain.com")
                      .name("test user 3")));

      List<UserDto> usersById = userService.getUsersByIds(requestedUserIds, currentPrincipal);

      assertFalse(usersById.isEmpty());
      assertEquals(requestedUserId1, usersById.get(0).getId());
      assertEquals(
          OffsetDateTime.parse("2022-01-01T00:00:00Z"), usersById.get(0).getPictureUpdatedAt());
      assertEquals("my status 1", usersById.get(0).getStatusMessage());
      assertEquals("test user 1", usersById.get(0).getName());
      assertEquals("test1@example.com", usersById.get(0).getEmail());
      assertEquals(requestedUserId2, usersById.get(1).getId());
      assertEquals(
          OffsetDateTime.parse("2022-02-02T00:00:00Z"), usersById.get(1).getPictureUpdatedAt());
      assertEquals("my status 2", usersById.get(1).getStatusMessage());
      assertEquals("test user 2", usersById.get(1).getName());
      assertEquals("test2@example.com", usersById.get(1).getEmail());
      assertEquals(requestedUserId3, usersById.get(2).getId());
      assertEquals(
          OffsetDateTime.parse("2022-03-03T00:00:00Z"), usersById.get(2).getPictureUpdatedAt());
      assertEquals("my status 3", usersById.get(2).getStatusMessage());
      assertEquals("test user 3", usersById.get(2).getName());
      assertEquals("test3@example.com", usersById.get(2).getEmail());
    }

    @Test
    @DisplayName("Don't returns duplicates")
    public void getUsersByIds_testNoDuplicates() {
      UUID requestedUserId1 = UUID.randomUUID();
      List<String> requestedUserIds =
          Arrays.asList(requestedUserId1.toString(), requestedUserId1.toString());

      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId1);

      when(userRepository.getByIds(requestedUserIds))
          .thenReturn(
              Collections.singletonList(
                  User.create()
                      .id(requestedUserId1.toString())
                      .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
                      .statusMessage("my status 1")));
      when(profilingService.getByIds(currentPrincipal, requestedUserIds))
          .thenReturn(
              Collections.singletonList(
                  UserProfile.create(requestedUserId1)
                      .email("test1@example.com")
                      .domain("mydomain.com")
                      .name("test user 1")));

      List<UserDto> usersById = userService.getUsersByIds(requestedUserIds, currentPrincipal);

      assertFalse(usersById.isEmpty());
      assertEquals(1, usersById.size());
    }

    @Test
    @DisplayName("Returns empty list")
    public void getUsersByIds_testEmptyList() {
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(userRepository.getByIds(Collections.emptyList())).thenReturn(Collections.emptyList());
      when(profilingService.getByIds(currentPrincipal, Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      assertTrue(userService.getUsersByIds(Collections.emptyList(), currentPrincipal).isEmpty());
    }

    @Test
    @DisplayName("Returns the user with the profiling info if it's not found in out db")
    public void getUsersByIds_testDbNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getByIds(Collections.singletonList(requestedUserId.toString())))
          .thenReturn(Collections.emptyList());
      when(profilingService.getByIds(
              currentPrincipal, Collections.singletonList(requestedUserId.toString())))
          .thenReturn(
              Collections.singletonList(
                  UserProfile.create(requestedUserId)
                      .email("test@example.com")
                      .domain("mydomain.com")
                      .name("test user")));

      List<UserDto> usersById =
          userService.getUsersByIds(
              Collections.singletonList(requestedUserId.toString()), currentPrincipal);

      assertFalse(usersById.isEmpty());
      assertEquals(requestedUserId, usersById.get(0).getId());
      assertNull(usersById.get(0).getPictureUpdatedAt());
      assertNull(usersById.get(0).getStatusMessage());
      assertEquals("test user", usersById.get(0).getName());
      assertEquals("test@example.com", usersById.get(0).getEmail());
    }

    @Test
    @DisplayName("Return an empty list if the user was not found using the profiling service")
    public void getUserById_testProfilingNotFound() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getByIds(Collections.singletonList(requestedUserId.toString())))
          .thenReturn(Collections.emptyList());
      when(profilingService.getByIds(
              currentPrincipal, Collections.singletonList(requestedUserId.toString())))
          .thenReturn(Collections.emptyList());

      List<UserDto> usersById =
          userService.getUsersByIds(
              Collections.singletonList(requestedUserId.toString()), currentPrincipal);

      assertTrue(usersById.isEmpty());
    }
  }

  @Nested
  @DisplayName("User exists tests")
  class UserExistsTests {

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    public void userExists_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(Optional.of(UserProfile.create(requestedUserId)));
      assertTrue(userService.userExists(requestedUserId, currentPrincipal));
    }

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    public void userExists_testUserDoesNotExist() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId))
          .thenReturn(Optional.empty());

      assertFalse(userService.userExists(requestedUserId, currentPrincipal));
    }
  }

  @Nested
  @DisplayName("Get user picture tests")
  class GetUserPictureTests {

    @Test
    @DisplayName("It returns the user picture")
    void getUserPicture_testOk() {
      UUID userId = UUID.randomUUID();
      FileMetadata pfpMetadata =
          FileMetadata.create()
              .type(FileMetadataType.USER_AVATAR)
              .userId(userId.toString())
              .mimeType("mime/type")
              .id(userId.toString())
              .name("pfp")
              .originalSize(123L);
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.of(pfpMetadata));
      File file = new File("test");
      when(storagesService.getFileById(userId.toString(), userId.toString())).thenReturn(file);

      FileContentAndMetadata picture =
          userService.getUserPicture(userId, UserPrincipal.create(userId));

      assertEquals(file, picture.getFile());
      assertEquals(pfpMetadata.getId(), picture.getMetadata().getId());
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(storagesService, times(1)).getFileById(userId.toString(), userId.toString());
    }

    @Test
    @DisplayName("If the user hasn't its picture, it throws a BadRequestException")
    void getUserPicture_fileNotFound() {
      UUID userId = UUID.randomUUID();
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> userService.getUserPicture(userId, UserPrincipal.create(userId)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - File with id '%s' not found", userId), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Set user picture tests")
  class SetUserPictureTests {

    @Test
    @DisplayName("It sets the user picture if it didn't exists")
    void setUserPicture_testOkInsert() {
      UUID userId = UUID.randomUUID();
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.empty());
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      List<String> contactsIds = List.of("a", "b", "c");
      when(subscriptionRepository.getContacts(userId.toString())).thenReturn(contactsIds);
      when(userRepository.getById(userId.toString())).thenReturn(Optional.empty());
      when(userRepository.save(
              User.create()
                  .id(userId.toString())
                  .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))))
          .thenReturn(
              User.create()
                  .id(userId.toString())
                  .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z")));

      userService.setUserPicture(
          userId, file, "image/jpeg", "picture", UserPrincipal.create(userId));

      FileMetadata expectedMetadata =
          FileMetadataBuilder.create()
              .id(userId.toString())
              .mimeType("image/jpeg")
              .type(FileMetadataType.USER_AVATAR)
              .name("picture")
              .originalSize(123L)
              .userId(userId.toString())
              .build();
      verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB);
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(userRepository, times(1)).getById(userId.toString());
      verify(userRepository, times(1))
          .save(
              User.create()
                  .id(userId.toString())
                  .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z")));
      verify(clock, times(1)).instant();
      verify(clock, times(1)).getZone();
      verify(subscriptionRepository, times(1)).getContacts(userId.toString());
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, userId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              contactsIds,
              UserPictureChanged.create()
                  .userId(userId)
                  .imageId(UUID.fromString(expectedMetadata.getId()))
                  .updatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z")));
      verifyNoMoreInteractions(
          fileMetadataRepository,
          userRepository,
          storagesService,
          subscriptionRepository,
          eventDispatcher,
          clock,
          appConfig);
      verifyNoInteractions(profilingService);
    }

    @Test
    @DisplayName("It update the user picture if it already exists")
    void setUserPicture_testOkUpdate() {
      UUID userId = UUID.randomUUID();
      when(fileMetadataRepository.getById(userId.toString()))
          .thenReturn(
              Optional.of(
                  FileMetadata.create()
                      .id(userId.toString())
                      .type(FileMetadataType.USER_AVATAR)
                      .userId(userId.toString())));
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      List<String> contactsIds = List.of("a", "b", "c");
      when(subscriptionRepository.getContacts(userId.toString())).thenReturn(contactsIds);
      User user =
          User.create()
              .id(userId.toString())
              .pictureUpdatedAt(OffsetDateTime.parse("2000-12-31T00:00:00Z"));
      when(userRepository.getById(userId.toString())).thenReturn(Optional.of(user));
      when(userRepository.save(
              User.create()
                  .id(userId.toString())
                  .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))))
          .thenReturn(
              User.create()
                  .id(userId.toString())
                  .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z")));

      userService.setUserPicture(
          userId, file, "image/jpeg", "picture", UserPrincipal.create(userId));

      FileMetadata expectedMetadata =
          FileMetadataBuilder.create()
              .id(userId.toString())
              .mimeType("image/jpeg")
              .type(FileMetadataType.USER_AVATAR)
              .name("picture")
              .originalSize(123L)
              .userId(userId.toString())
              .build();
      verify(appConfig, times(1)).get(Integer.class, ConfigName.MAX_USER_IMAGE_SIZE_IN_KB);
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(clock, times(1)).instant();
      verify(clock, times(1)).getZone();
      verify(userRepository, times(1)).getById(userId.toString());
      verify(userRepository, times(1))
          .save(user.pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z")));
      verify(subscriptionRepository, times(1)).getContacts(userId.toString());
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, userId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              contactsIds,
              UserPictureChanged.create()
                  .userId(userId)
                  .imageId(UUID.fromString(expectedMetadata.getId()))
                  .updatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z")));
      verifyNoMoreInteractions(
          fileMetadataRepository,
          userRepository,
          storagesService,
          subscriptionRepository,
          eventDispatcher,
          clock,
          appConfig);
      verifyNoInteractions(profilingService);
    }

    @Test
    @DisplayName("If the picture is not an image, It throws a BadRequestException")
    void setUserPicture_failsIfPictureIsNotAnImage() {
      UUID userId = UUID.randomUUID();
      File file = mock(File.class);
      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  userService.setUserPicture(
                      userId, file, "text/html", "picture", UserPrincipal.create(userId)));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - The user picture must be an image", exception.getMessage());
    }

    @Test
    @DisplayName("If the file is too large, It throws a 'bad request' exception")
    void setUserPicture_failsIfPictureIsTooBig() {
      UUID userId = UUID.randomUUID();
      File file = mock(File.class);
      when(file.length()).thenReturn(600L * 1024);
      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  userService.setUserPicture(
                      userId, file, "image/jpeg", "picture", UserPrincipal.create(userId)));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Bad Request - The user picture cannot be greater than %d kB", 512),
          exception.getMessage());
    }

    @Test
    @DisplayName(
        "If the specified user is not the authenticated user, it throws a ForbiddenException")
    void setUserPicture_failsIfUserIsNotAuthenticatedUser() {
      UUID user1Id = UUID.randomUUID();
      UUID user2Id = UUID.randomUUID();
      File file = mock(File.class);
      ForbiddenException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  userService.setUserPicture(
                      user2Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          "Forbidden - The picture can be change only from its owner", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Delete user picture tests")
  class DeleteUserPictureTests {

    @Test
    @DisplayName("Correctly deletes the user picture")
    public void deleteUserPicture_testOk() {
      UUID userId = UUID.randomUUID();
      FileMetadata metadata = FileMetadata.create().id(userId.toString()).userId(userId.toString());
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.of(metadata));
      List<String> contacts = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
      when(subscriptionRepository.getContacts(userId.toString())).thenReturn(contacts);
      User user =
          User.create()
              .id(userId.toString())
              .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"));
      when(userRepository.getById(userId.toString())).thenReturn(Optional.of(user));
      userService.deleteUserPicture(userId, UserPrincipal.create(userId));

      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).delete(metadata);
      verify(userRepository, times(1)).getById(userId.toString());
      verify(userRepository, times(1)).save(user.pictureUpdatedAt(null));
      verify(storagesService, times(1)).deleteFile(userId.toString(), userId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(eq(contacts), any(UserPictureDeleted.class));
      verify(subscriptionRepository, times(1)).getContacts(userId.toString());

      verifyNoMoreInteractions(
          fileMetadataRepository,
          userRepository,
          storagesService,
          eventDispatcher,
          subscriptionRepository,
          eventDispatcher);
      verifyNoInteractions(profilingService, appConfig, clock);
    }

    @Test
    @DisplayName("Correctly deletes the user picture by a system user")
    public void deleteUserPicture_bySystemUser() {
      UUID userId = UUID.randomUUID();
      FileMetadata metadata = FileMetadata.create().id(userId.toString()).userId(userId.toString());
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.of(metadata));
      List<String> contacts = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
      when(subscriptionRepository.getContacts(userId.toString())).thenReturn(contacts);

      userService.deleteUserPicture(
          userId, UserPrincipal.create(UUID.randomUUID()).systemUser(true));

      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).delete(metadata);
      verify(storagesService, times(1)).deleteFile(userId.toString(), userId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(eq(contacts), any(UserPictureDeleted.class));
      verify(subscriptionRepository, times(1)).getContacts(userId.toString());
      verifyNoMoreInteractions(
          fileMetadataRepository, storagesService, eventDispatcher, subscriptionRepository);
    }

    @Test
    @DisplayName("If user is not the picture owner, it throws a ForbiddenException")
    public void deleteUserPicture_userNotPictureOwner() {
      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  userService.deleteUserPicture(
                      UUID.randomUUID(), UserPrincipal.create(UUID.randomUUID())));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          "Forbidden - The picture can be removed only from its owner", exception.getMessage());
      verifyNoInteractions(
          fileMetadataRepository, storagesService, eventDispatcher, subscriptionRepository);
    }

    @Test
    @DisplayName("If the user hasn't its picture, it throws a BadRequestException")
    public void deleteUserPicture_fileNotFound() {
      UUID userId = UUID.randomUUID();
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> userService.getUserPicture(userId, UserPrincipal.create(userId)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - File with id '%s' not found", userId), exception.getMessage());
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verifyNoMoreInteractions(fileMetadataRepository);
      verifyNoInteractions(storagesService, eventDispatcher, subscriptionRepository);
    }
  }
}
