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
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.User;
import com.zextras.carbonio.chats.core.data.event.UserPictureChangedEvent;
import com.zextras.carbonio.chats.core.data.event.UserPictureDeletedEvent;
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
import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class UserServiceImplTest {

  private final UserService            userService;
  private final ProfilingService       profilingService;
  private final UserRepository         userRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService        storagesService;
  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher        eventDispatcher;
  private final AppConfig              appConfig;

  public UserServiceImplTest() {
    this.profilingService = mock(ProfilingService.class);
    this.userRepository = mock(UserRepository.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.appConfig = mock(AppConfig.class);
    this.userService = new UserServiceImpl(
      profilingService,
      userRepository,
      fileMetadataRepository,
      storagesService,
      subscriptionRepository,
      eventDispatcher,
      appConfig
    );
  }

  @Nested
  @DisplayName("Get user by id tests")
  class GetUserByIdTests {

    @Test
    @DisplayName("Returns the user with every info")
    public void getUserById_testOk() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(requestedUserId);
      when(userRepository.getById(requestedUserId.toString())).thenReturn(
        Optional.of(
          User.create().id(requestedUserId.toString())
            .lastSeen(OffsetDateTime.of(2022, 12, 1, 12, 12, 12, 12, ZoneOffset.UTC))
            .hash("12345").statusMessage("my status!"))
      );
      when(profilingService.getById(currentPrincipal, requestedUserId))
        .thenReturn(Optional.of(
          UserProfile.create(requestedUserId).email("test@example.com").domain("mydomain.com").name("test user")));

      UserDto userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertEquals(OffsetDateTime.of(2022, 12, 1, 12, 12, 12, 12, ZoneOffset.UTC).toEpochSecond(),
        userById.getLastSeen());
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
        .thenReturn(Optional.of(
          UserProfile.create(requestedUserId).email("test@example.com").domain("mydomain.com").name("test user"))
        );

      UserDto userById = userService.getUserById(requestedUserId, currentPrincipal);

      assertNotNull(userById);
      assertEquals(requestedUserId, userById.getId());
      assertNull(userById.getLastSeen());
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
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(Optional.empty());

      assertThrows(NotFoundException.class, () -> userService.getUserById(requestedUserId, currentPrincipal));
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
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(
        Optional.of(UserProfile.create(requestedUserId)));
      assertTrue(userService.userExists(requestedUserId, currentPrincipal));
    }

    @Test
    @DisplayName("Returns true if the user exists in the profiler")
    public void userExists_testUserDoesNotExist() {
      UUID requestedUserId = UUID.randomUUID();
      UserPrincipal currentPrincipal = UserPrincipal.create(UUID.randomUUID());
      when(profilingService.getById(currentPrincipal, requestedUserId)).thenReturn(Optional.empty());

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
      FileMetadata pfpMetadata = FileMetadata.create()
        .type(FileMetadataType.USER_AVATAR)
        .userId(userId.toString())
        .mimeType("mime/type")
        .id(userId.toString())
        .name("pfp").originalSize(123L);
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.of(pfpMetadata));
      File file = new File("test");
      when(storagesService.getFileById(userId.toString(), userId.toString())).thenReturn(file);

      FileContentAndMetadata picture = userService.getUserPicture(userId, UserPrincipal.create(userId));

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

      ChatsHttpException exception = assertThrows(NotFoundException.class,
        () -> userService.getUserPicture(userId, UserPrincipal.create(userId)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - File with id '%s' not found", userId),
        exception.getMessage());
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

      userService.setUserPicture(userId, file, "image/jpeg", "picture", UserPrincipal.create(userId));

      FileMetadata expectedMetadata = FileMetadataBuilder.create().id(userId.toString())
        .mimeType("image/jpeg").type(FileMetadataType.USER_AVATAR).name("picture").originalSize(123L)
        .userId(userId.toString()).build();
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, userId.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(contactsIds,
        UserPictureChangedEvent.create(userId).userId(userId));
      verifyNoMoreInteractions(fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("It update the user picture if it already exists")
    void setUserPicture_testOkUpdate() {
      UUID userId = UUID.randomUUID();
      when(fileMetadataRepository.getById(userId.toString()))
        .thenReturn(Optional.of(FileMetadata.create()
          .id(userId.toString())
          .type(FileMetadataType.USER_AVATAR)
          .userId(userId.toString())));
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      List<String> contactsIds = List.of("a", "b", "c");
      when(subscriptionRepository.getContacts(userId.toString())).thenReturn(contactsIds);

      userService.setUserPicture(userId, file, "image/jpeg", "picture", UserPrincipal.create(userId));

      FileMetadata expectedMetadata = FileMetadataBuilder.create().id(userId.toString())
        .mimeType("image/jpeg").type(FileMetadataType.USER_AVATAR).name("picture").originalSize(123L)
        .userId(userId.toString()).build();
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, userId.toString());
      verify(eventDispatcher, times(1)).sendToUserQueue(contactsIds,
        UserPictureChangedEvent.create(userId).userId(userId));
      verifyNoMoreInteractions(fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If the picture is not an image, It throws a BadRequestException")
    void setUserPicture_failsIfPictureIsNotAnImage() {
      UUID userId = UUID.randomUUID();
      File file = mock(File.class);
      ChatsHttpException exception = assertThrows(BadRequestException.class,
        () -> userService.setUserPicture(userId, file, "text/html", "picture", UserPrincipal.create(userId)));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - The user picture must be an image",
        exception.getMessage());
    }

    @Test
    @DisplayName("If the file is too large, It throws a 'bad request' exception")
    void setUserPicture_failsIfPictureIsTooBig() {
      UUID userId = UUID.randomUUID();
      File file = mock(File.class);
      when(file.length()).thenReturn(257L * 1024);
      ChatsHttpException exception = assertThrows(BadRequestException.class,
        () -> userService.setUserPicture(userId, file, "image/jpeg", "picture", UserPrincipal.create(userId)));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Bad Request - The user picture cannot be greater than %d KB", 256),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the specified user is not the authenticated user, it throws a ForbiddenException")
    void setUserPicture_failsIfUserIsNotAuthenticatedUser() {
      UUID user1Id = UUID.randomUUID();
      UUID user2Id = UUID.randomUUID();
      File file = mock(File.class);
      ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> userService.setUserPicture(user2Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Forbidden - The picture can be change only from its owner", exception.getMessage());
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

      userService.deleteUserPicture(userId, UserPrincipal.create(userId));

      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).delete(metadata);
      verify(storagesService, times(1)).deleteFile(userId.toString(), userId.toString());
      verify(eventDispatcher, times(1))
        .sendToUserQueue(eq(contacts), any(UserPictureDeletedEvent.class));
      verify(subscriptionRepository, times(1)).getContacts(userId.toString());
      verifyNoMoreInteractions(fileMetadataRepository, storagesService, eventDispatcher, subscriptionRepository);
    }

    @Test
    @DisplayName("Correctly deletes the user picture by a system user")
    public void deleteUserPicture_bySystemUser() {
      UUID userId = UUID.randomUUID();
      FileMetadata metadata = FileMetadata.create().id(userId.toString()).userId(userId.toString());
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.of(metadata));
      List<String> contacts = List.of(UUID.randomUUID().toString(), UUID.randomUUID().toString());
      when(subscriptionRepository.getContacts(userId.toString())).thenReturn(contacts);

      userService.deleteUserPicture(userId, UserPrincipal.create(UUID.randomUUID()).systemUser(true));

      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verify(fileMetadataRepository, times(1)).delete(metadata);
      verify(storagesService, times(1)).deleteFile(userId.toString(), userId.toString());
      verify(eventDispatcher, times(1))
        .sendToUserQueue(eq(contacts), any(UserPictureDeletedEvent.class));
      verify(subscriptionRepository, times(1)).getContacts(userId.toString());
      verifyNoMoreInteractions(fileMetadataRepository, storagesService, eventDispatcher, subscriptionRepository);
    }

    @Test
    @DisplayName("If user is not the picture owner, it throws a ForbiddenException")
    public void deleteUserPicture_userNotPictureOwner() {
      ChatsHttpException exception = assertThrows(ForbiddenException.class,
        () -> userService.deleteUserPicture(UUID.randomUUID(), UserPrincipal.create(UUID.randomUUID())));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Forbidden - The picture can be removed only from its owner", exception.getMessage());
      verifyNoInteractions(fileMetadataRepository, storagesService, eventDispatcher, subscriptionRepository);
    }

    @Test
    @DisplayName("If the user hasn't its picture, it throws a BadRequestException")
    public void deleteUserPicture_fileNotFound() {
      UUID userId = UUID.randomUUID();
      when(fileMetadataRepository.getById(userId.toString())).thenReturn(Optional.empty());

      ChatsHttpException exception = assertThrows(NotFoundException.class,
        () -> userService.getUserPicture(userId, UserPrincipal.create(userId)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - File with id '%s' not found", userId),
        exception.getMessage());
      verify(fileMetadataRepository, times(1)).getById(userId.toString());
      verifyNoMoreInteractions(fileMetadataRepository);
      verifyNoInteractions(storagesService, eventDispatcher, subscriptionRepository);
    }
  }
}