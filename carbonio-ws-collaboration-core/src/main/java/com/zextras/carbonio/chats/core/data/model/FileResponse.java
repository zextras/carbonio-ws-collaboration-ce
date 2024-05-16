// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import java.io.InputStream;

public class FileResponse {
  private final InputStream content;
  private final long        length;
  private final String      mimeType;

  public FileResponse(InputStream content, long length, String mimeType) {
    this.content = content;
    this.length = length;
    this.mimeType = mimeType;
  }

  public InputStream getContent() {
    return this.content;
  }

  public long getLength() {
    return this.length;
  }

  public String getMimeType() {
    return this.mimeType;
  }

}

