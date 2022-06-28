package com.zextras.carbonio.chats.core.aop;

import com.zextras.carbonio.chats.core.annotation.TimedCall;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.time.Duration;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TimedCallInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    String methodName = methodInvocation.getMethod().getAnnotation(TimedCall.class).name();
    if(methodName.isEmpty()) {
      methodName = methodInvocation.getMethod().getName();
    }
    long callStart = System.nanoTime();
    Object invocation = methodInvocation.proceed();
    ChatsLogger.debug(
      String.format("%s call lasted: %dms", methodName,
      Duration.ofNanos(System.nanoTime() - callStart).toMillis())
    );
    return invocation;
  }

}
