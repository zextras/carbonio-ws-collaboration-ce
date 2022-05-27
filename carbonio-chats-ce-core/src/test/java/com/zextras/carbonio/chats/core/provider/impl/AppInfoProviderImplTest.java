package com.zextras.carbonio.chats.core.provider.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.provider.AppInfoProvider;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
public class AppInfoProviderImplTest {

  private final AppInfoProvider appInfoProvider;

  public AppInfoProviderImplTest() {
    this.appInfoProvider = new AppInfoProviderImpl();
  }

  @Test
  @DisplayName("Correctly gets ")
  public void getVersion_testOk() {
    Optional<String> version = appInfoProvider.getVersion();
    assertTrue(version.isPresent());
    assertEquals("1.0.0", version.get());
  }
}
