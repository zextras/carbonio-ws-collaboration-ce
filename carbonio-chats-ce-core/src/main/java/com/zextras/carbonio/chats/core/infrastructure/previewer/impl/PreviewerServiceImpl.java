// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

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
