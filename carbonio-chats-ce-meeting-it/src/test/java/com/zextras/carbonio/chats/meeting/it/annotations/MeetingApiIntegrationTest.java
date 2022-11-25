package com.zextras.carbonio.chats.meeting.it.annotations;

import com.zextras.carbonio.chats.it.extensions.DatabaseExtension;
import com.zextras.carbonio.chats.it.extensions.RestEasyExtension;
import com.zextras.carbonio.chats.it.extensions.UserManagementExtension;
import com.zextras.carbonio.chats.meeting.it.extensions.MeetingGuiceExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(MeetingGuiceExtension.class)
@ExtendWith(RestEasyExtension.class)
@ExtendWith(DatabaseExtension.class)
@ExtendWith(UserManagementExtension.class)
public @interface MeetingApiIntegrationTest {

}
