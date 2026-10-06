// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.logging.annotation;

import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@InterceptorBinding
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TimedCall {
  @Nonbinding
  String name() default "";

  @Nonbinding
  ChatsLoggerLevel logLevel() default ChatsLoggerLevel.DEBUG;
}
