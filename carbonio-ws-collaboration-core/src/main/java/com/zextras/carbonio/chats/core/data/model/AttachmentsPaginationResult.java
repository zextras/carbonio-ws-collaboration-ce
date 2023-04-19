// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttachmentsPaginationResult {

  private List<FileMetadata> attachments = new ArrayList<>();
  private String             filter;

  public static AttachmentsPaginationResult create() {
    return new AttachmentsPaginationResult();
  }

  public List<FileMetadata> getAttachments() {
    return Collections.unmodifiableList(attachments);
  }

  public AttachmentsPaginationResult attachments(List<FileMetadata> attachments) {
    this.attachments = attachments;
    return this;
  }

  public String getFilter() {
    return filter;
  }

  public AttachmentsPaginationResult filter(String filter) {
    this.filter = filter;
    return this;
  }
}
