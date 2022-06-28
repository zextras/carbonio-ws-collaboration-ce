package com.zextras.carbonio.chats.core.logging.annotation;

import com.zextras.carbonio.chats.core.logging.ChatsLoggerLevel;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.METHOD)
public @interface TimedCall {
  String name() default "";
  ChatsLoggerLevel logLevel() default ChatsLoggerLevel.DEBUG;
}
