// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
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
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@TimedCall
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class TimedCallInterceptor {

  @AroundInvoke
  public Object invoke(InvocationContext methodInvocation) throws Exception {
    TimedCall annotation = methodInvocation.getMethod().getAnnotation(TimedCall.class);
    ChatsLoggerLevel loggerLevel = annotation.logLevel();
    String methodName = annotation.name();
    if (methodName.isEmpty()) {
      methodName = methodInvocation.getMethod().getName();
    }
    long callStart = System.nanoTime();
    Object invocation = methodInvocation.proceed();
    String parameterString =
        Arrays.stream(methodInvocation.getParameters())
            .map(argument -> Optional.ofNullable(argument).map(Object::toString).orElse("null"))
            .collect(Collectors.joining(", "));
    ChatsLogger.log(
        loggerLevel,
        this.getClass(),
        String.format(
            "%s [ %s ] call lasted: %dms",
            methodName,
            parameterString,
            Duration.ofNanos(System.nanoTime() - callStart).toMillis()),
        null);
    return invocation;
  }
}
