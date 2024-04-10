// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.annotations;

import com.zextras.carbonio.chats.it.extensions.ConsulExtension;
import com.zextras.carbonio.chats.it.extensions.DatabaseExtension;
import com.zextras.carbonio.chats.it.extensions.GuiceExtension;
import com.zextras.carbonio.chats.it.extensions.MongooseIMExtension;
import com.zextras.carbonio.chats.it.extensions.PreviewerExtension;
import com.zextras.carbonio.chats.it.extensions.RestEasyExtension;
import com.zextras.carbonio.chats.it.extensions.StoragesExtension;
import com.zextras.carbonio.chats.it.extensions.UserManagementExtension;
import com.zextras.carbonio.chats.it.extensions.VideoRecorderExtension;
import com.zextras.carbonio.chats.it.extensions.VideoServerExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(GuiceExtension.class)
@ExtendWith(RestEasyExtension.class)
@ExtendWith(DatabaseExtension.class)
@ExtendWith(StoragesExtension.class)
@ExtendWith(PreviewerExtension.class)
@ExtendWith(UserManagementExtension.class)
@ExtendWith(MongooseIMExtension.class)
@ExtendWith(VideoServerExtension.class)
@ExtendWith(VideoRecorderExtension.class)
@ExtendWith(ConsulExtension.class)
public @interface ApiIntegrationTest {}
