// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.logging.aop;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TimedCallInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    TimedCall annotation = methodInvocation.getMethod().getAnnotation(TimedCall.class);
    ChatsLoggerLevel loggerLevel = annotation.logLevel();
    String methodName = annotation.name();
    if (methodName.isEmpty()) {
      methodName = methodInvocation.getMethod().getName();
    }
    long callStart = System.nanoTime();
    Object invocation = methodInvocation.proceed();
    String parameterString = Arrays.stream(methodInvocation.getArguments())
      .map(argument -> Optional.ofNullable(argument).map(Object::toString).orElse("null"))
      .collect(Collectors.joining(", "));
    ChatsLogger.log(
      loggerLevel,
      this.getClass(),
      String.format("%s [ %s ] call lasted: %dms", methodName, parameterString,
        Duration.ofNanos(System.nanoTime() - callStart).toMillis()),
      null
    );
    return invocation;
  }

}
