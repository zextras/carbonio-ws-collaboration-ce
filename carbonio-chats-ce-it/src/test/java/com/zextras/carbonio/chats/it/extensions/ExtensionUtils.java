package com.zextras.carbonio.chats.it.extensions;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.ModifierSupport;

public class ExtensionUtils {

  public static boolean isNestedClass(ExtensionContext context) {
    return context.getTestClass().isPresent() &&
      !ModifierSupport.isStatic(context.getTestClass().get())
      && context.getTestClass().get().isMemberClass();
  }
}
