package com.zextras.carbonio.chats.meeting.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.zextras.carbonio.chats.mongooseim.admin.invoker.ApiClient;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.storages.api.StoragesClient;
import javax.inject.Singleton;

/**
 * This class aim is to override the guice binds for the core module
 */
public class TestModuleOverwrite extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
  }

  /**
   * Overrides MongooseIm client with a mock
   *
   * @return an empty MongooseIm client {@link ApiClient}
   */
  @Singleton
  @Provides
  private ApiClient getMongooseImAdminApiClient() {
    return new ApiClient();
  }

  /**
   * Overrides storage client with a fake url
   *
   * @return not usable storage client {@link StoragesClient}
   */
  @Singleton
  @Provides
  private StoragesClient getStoragesClient() {
    return StoragesClient.atUrl("http://localhost:6794");
  }

  /**
   * Overrides preview client with a fake url
   *
   * @return not usable preview client {@link PreviewClient}
   */
  @Singleton
  @Provides
  private PreviewClient getPreviewClient() {
    return PreviewClient.atURL("http://localhost:7894");
  }
}