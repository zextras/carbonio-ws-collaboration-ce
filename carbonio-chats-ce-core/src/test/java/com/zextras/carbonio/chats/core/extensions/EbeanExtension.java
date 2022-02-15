package com.zextras.carbonio.chats.core.extensions;

import io.ebean.test.ForTests;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EbeanExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    ForTests.enableTransactional(false);
  }
}