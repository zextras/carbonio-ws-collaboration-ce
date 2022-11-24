package com.zextras.carbonio.chats.meeting.extensions;

import io.ebean.test.ForTests;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EbeanTestExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    ForTests.enableTransactional(false);
  }
}
