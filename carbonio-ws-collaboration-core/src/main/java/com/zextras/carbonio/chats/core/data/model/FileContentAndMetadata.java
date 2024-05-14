// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import java.io.InputStream;

public class FileContentAndMetadata {

  private final InputStream  fileStream;
  private final FileMetadata metadata;

  public FileContentAndMetadata(InputStream fileStream, FileMetadata metadata) {
    this.fileStream = fileStream;
    this.metadata = metadata;
  }

  public InputStream getFileStream() {
    return fileStream;
  }

  public FileMetadata getMetadata() {
    return metadata;
  }
}
