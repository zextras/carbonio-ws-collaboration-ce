package com.zextras.carbonio.chats.core.infrastructure.previewer.impl;

import com.zextras.carbonio.chats.core.infrastructure.previewer.PreviewerService;
import com.zextras.carbonio.preview.PreviewClient;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PreviewerServiceImpl implements PreviewerService {

  private final PreviewClient previewClient;

  @Inject
  public PreviewerServiceImpl(PreviewClient previewClient) {
    this.previewClient = previewClient;
  }

  @Override
  public boolean isAlive() {
    return previewClient.healthReady();
  }
}
