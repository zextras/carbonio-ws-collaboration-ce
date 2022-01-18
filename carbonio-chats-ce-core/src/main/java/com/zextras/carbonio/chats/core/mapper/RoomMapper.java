package com.zextras.carbonio.chats.core.mapper;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomResponseDto;
import com.zextras.carbonio.chats.model.RoomUserSettingsDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "jsr330", imports = {ArrayList.class, UUID.class, RoomUserSettingsDto.class})
public abstract class RoomMapper {

  @Inject
  protected SubscriptionMapper subscriptionMapper;

  @Inject
  protected RoomUserSettingsMapper roomUserSettingsMapper;


  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))")
  })
  public abstract RoomDto ent2roomDto(Room room);

  public abstract List<RoomDto> ent2roomDto(List<Room> room);

  @Mappings({
    @Mapping(target = "id", expression = "java(UUID.fromString(room.getId()))"),
    @Mapping(target = "members", expression = "java(subscriptionMapper.ent2memberDto(room.getSubscriptions()))"),
    @Mapping(target = "userSettings", expression = "java(roomUserSettingsMapper.ent2dto(room.getUserSettings(), userId))")
  })
  public abstract RoomResponseDto ent2roomInfoDto(Room room, String userId);

}
