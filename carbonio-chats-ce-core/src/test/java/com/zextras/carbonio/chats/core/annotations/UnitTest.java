package com.zextras.carbonio.chats.core.annotations;

import com.zextras.carbonio.chats.core.extensions.EbeanTestExtension;
import com.zextras.carbonio.chats.core.extensions.GuiceMappersExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(GuiceMappersExtension.class)
@ExtendWith(EbeanTestExtension.class)
public @interface UnitTest { }
