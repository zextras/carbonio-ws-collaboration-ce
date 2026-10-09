// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static com.zextras.carbonio.chats.core.config.module.CoreModule.URL_PATTERN;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.api.AttachmentsApi;
import com.zextras.carbonio.chats.api.AttachmentsApiService;
import com.zextras.carbonio.chats.api.PreviewApi;
import com.zextras.carbonio.chats.api.PreviewApiService;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.infrastructure.preview.PreviewService;
import com.zextras.carbonio.chats.core.infrastructure.preview.impl.PreviewServiceImpl;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.infrastructure.storage.impl.StoragesServiceImpl;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.mapper.impl.AttachmentMapperImpl;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.impl.EbeanFileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.impl.AttachmentServiceImpl;
import com.zextras.carbonio.chats.core.web.api.AttachmentsApiServiceImpl;
import com.zextras.carbonio.chats.core.web.api.PreviewApiServiceImpl;
import com.zextras.carbonio.preview.sdk.PreviewClient;
import com.zextras.storages.api.StoragesClient;

public class AttachmentModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(AttachmentsApi.class);
    bind(AttachmentsApiService.class).to(AttachmentsApiServiceImpl.class);
    bind(AttachmentService.class).to(AttachmentServiceImpl.class);
    bind(AttachmentMapper.class).to(AttachmentMapperImpl.class);
    bind(FileMetadataRepository.class).to(EbeanFileMetadataRepository.class);
    bind(StoragesService.class).to(storagesService());

    bind(PreviewApi.class);
    bind(PreviewApiService.class).to(PreviewApiServiceImpl.class);
    bind(PreviewService.class).to(PreviewServiceImpl.class);
  }

  protected Class<? extends StoragesService> storagesService() {
    return StoragesServiceImpl.class;
  }

  @Singleton
  @Provides
  private StoragesClient getStoragesClient(AppConfig appConfig) {
    return StoragesClient.atUrl(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.STORAGES_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.STORAGES_PORT).orElseThrow()));
  }

  @Singleton
  @Provides
  private PreviewClient getPreviewClient(AppConfig appConfig) {
    return PreviewClient.atURL(
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.PREVIEWER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.PREVIEWER_PORT).orElseThrow()));
  }
}
