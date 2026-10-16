// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.zextras.carbonio.chats.api.HealthApi;
import com.zextras.carbonio.chats.api.HealthApiService;
import com.zextras.carbonio.chats.api.OffsetDateTimeProvider;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.config.impl.CoreConfigContribution;
import com.zextras.carbonio.chats.core.logging.annotation.TimedCall;
import com.zextras.carbonio.chats.core.logging.aop.TimedCallInterceptor;
import com.zextras.carbonio.chats.core.service.HealthcheckService;
import com.zextras.carbonio.chats.core.service.impl.HealthcheckServiceImpl;
import com.zextras.carbonio.chats.core.web.api.HealthApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.versioning.filter.VersionedRequestFilter;
import com.zextras.carbonio.chats.core.web.api.versioning.filter.VersionedResponseFilter;
import com.zextras.carbonio.chats.core.web.exceptions.ChatsHttpExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.ClientErrorExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.DefaultExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.JsonProcessingExceptionHandler;
import com.zextras.carbonio.chats.core.web.exceptions.ValidationExceptionHandler;
import com.zextras.carbonio.chats.core.web.security.AuthenticationFilter;
import java.time.Clock;
import java.time.ZoneId;

public class WebModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ConfigContribution.class)
        .addBinding()
        .to(CoreConfigContribution.class);

    // This is bound twice, once for RestEasy injection and one for everything else
    bind(JacksonConfig.class);
    bind(ObjectMapper.class).toProvider(JacksonConfig.class);

    bind(OffsetDateTimeProvider.class);
    bind(AuthenticationFilter.class);
    bind(VersionedResponseFilter.class);
    bind(VersionedRequestFilter.class);

    bindInterceptor(
        Matchers.any(), Matchers.annotatedWith(TimedCall.class), new TimedCallInterceptor());

    bind(ChatsHttpExceptionHandler.class);
    bind(ClientErrorExceptionHandler.class);
    bind(JsonProcessingExceptionHandler.class);
    bind(DefaultExceptionHandler.class);
    bind(ValidationExceptionHandler.class);

    bind(HealthApi.class);
    bind(HealthApiService.class).to(HealthApiServiceImpl.class);
    bind(HealthcheckService.class).to(HealthcheckServiceImpl.class);
  }

  @Singleton
  @Provides
  private Clock getClock() {
    return Clock.system(ZoneId.systemDefault());
  }
}
