// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.ModifierSupport;

public class ExtensionUtils {

  public static boolean isNestedClass(ExtensionContext context) {
    return context.getTestClass().isPresent() &&
      !ModifierSupport.isStatic(context.getTestClass().get())
      && context.getTestClass().get().isMemberClass();
  }
}
