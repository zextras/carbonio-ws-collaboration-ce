package com.zextras.carbonio.chats.core.logging.aop;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import java.time.Duration;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TimedCallInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    TimedCall annotation = methodInvocation.getMethod().getAnnotation(TimedCall.class);
    ChatsLoggerLevel loggerLevel = annotation.logLevel();
    String methodName = annotation.name();
    if(methodName.isEmpty()) {
      methodName = methodInvocation.getMethod().getName();
    }
    long callStart = System.nanoTime();
    Object invocation = methodInvocation.proceed();
    ChatsLogger.log(
      loggerLevel,
      this.getClass(),
      String.format("%s call lasted: %dms", methodName,
      Duration.ofNanos(System.nanoTime() - callStart).toMillis()),
      null
    );
    return invocation;
  }

}
