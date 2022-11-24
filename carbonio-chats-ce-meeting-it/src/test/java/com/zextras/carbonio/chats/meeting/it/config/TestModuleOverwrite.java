package com.zextras.carbonio.chats.meeting.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.ApiClient;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.storages.api.StoragesClient;
import com.zextras.storages.internal.client.StoragesClientImp;
import javax.inject.Singleton;

public class TestModuleOverwrite extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
  }

  @Singleton
  @Provides
  private ApiClient getMongooseImAdminApiClient() {
    return new ApiClient();
  }

  @Singleton
  @Provides
  private StoragesClient getStoragesClient() {
    return StoragesClient.atUrl("http://localhost:6794");
  }

  @Singleton
  @Provides
  private PreviewClient getPreviewClient() {
    return PreviewClient.atURL("http://localhost:7894");
  }
}