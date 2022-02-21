// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.model.AttachmentDto;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "jsr330", imports = {UUID.class})
public abstract class AttachmentMapper {

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(metadata.getId()))"),
    @Mapping(target = "size", source = "originalSize"),
    @Mapping(target = "userId", expression = "java(UUID.fromString(metadata.getUserId()))"),
    @Mapping(target = "roomId", expression = "java(UUID.fromString(metadata.getRoomId()))")
  })
  public abstract AttachmentDto ent2dto(FileMetadata metadata);

  public abstract List<AttachmentDto> ent2dto(List<FileMetadata> metadataList);


}
