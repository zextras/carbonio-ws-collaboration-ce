// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import java.io.File;

public class FileContentAndMetadata {

  private final File         file;
  private final FileMetadata metadata;

  public FileContentAndMetadata(File file, FileMetadata metadata) {
    this.file = file;
    this.metadata = metadata;
  }

  public File getFile() {
    return file;
  }

  public FileMetadata getMetadata() {
    return metadata;
  }
}
