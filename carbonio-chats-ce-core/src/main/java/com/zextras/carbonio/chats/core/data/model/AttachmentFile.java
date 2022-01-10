package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import java.io.File;

public class AttachmentFile {

  private File         file;
  private FileMetadata metadata;

  public AttachmentFile(File file, FileMetadata metadata) {
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
