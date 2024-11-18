// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

public class FileContentAndMetadata implements StreamingOutput {

  private final InputStream fileStream;
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

  @Override
  public void write(OutputStream outputStream) throws WebApplicationException {
    try {
      IOUtils.copyLarge(fileStream, outputStream);
    } catch (IOException e) {
      ChatsLogger.warn("Error occurred on the file stream " + metadata.getId(), e);
      throw new WebApplicationException("File streaming failed for " + metadata.getId(), e);
    } finally {
      if (fileStream != null) {
        try {
          fileStream.close();
        } catch (IOException e) {
          ChatsLogger.error("Failed to close the file stream " + metadata.getId(), e);
        }
      }
    }
  }
}
