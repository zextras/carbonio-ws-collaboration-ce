package com.zextras.carbonio.chats.core.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.User;
import io.ebean.Database;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class EbeanUserRepositoryTest {

  private EbeanUserRepository ebeanUserRepository;
  private Database            database;

  public EbeanUserRepositoryTest() {
    this.database = mock(Database.class, RETURNS_DEEP_STUBS);
    this.ebeanUserRepository = new EbeanUserRepository(database);
  }

  @AfterEach
  public void cleanup() {
    reset(database);
  }

  @Nested
  @DisplayName("Get by id test")
  class GetByIdTests {

    @Test
    @DisplayName("Retrieves a user by it's id")
    public void getById_testOK() {
      when(database.find(User.class).where().eq("id", "123").findOneOrEmpty()).thenReturn(
        Optional.of(User.create().id("123")));

      Optional<User> user = ebeanUserRepository.getById("123");

      assertTrue(user.isPresent());
      assertEquals("123", user.get().getId());
    }

    @Test
    @DisplayName("Returns an empty optional if the user was not found")
    public void getById_testNotFound() {
      when(database.find(User.class).where().eq("id", "123").findOneOrEmpty()).thenReturn(
        Optional.empty());
      Optional<User> user = ebeanUserRepository.getById("123");

      assertTrue(user.isEmpty());
    }

  }

}