// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;


import java.io.File;

public class FileResponse {
  private final File content;
  private final long length;
  private final String mimeType;

  public FileResponse(File content, long length, String mimeType) {
    this.content = content;
    this.length = length;
    this.mimeType = mimeType;
  }

  public File getContent() {
    return this.content;
  }

  public long getLength() {
    return this.length;
  }

  public String getMimeType() {
    return this.mimeType;
  }

}

