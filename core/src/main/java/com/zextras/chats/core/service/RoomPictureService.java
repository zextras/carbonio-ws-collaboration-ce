package com.zextras.chats.core.service;

import com.zextras.chats.core.data.entity.Room;
import java.io.File;

public interface RoomPictureService {

  void setPictureForRoom(Room room, File image);

}
