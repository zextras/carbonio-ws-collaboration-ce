package com.zextras.chats.core.service;

import com.zextras.chats.core.data.entity.Room;
import java.io.File;

public interface RoomPictureService {

  void save(Room room, File image);

}
