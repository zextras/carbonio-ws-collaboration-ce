package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import java.io.File;
import java.util.UUID;

public interface RoomPictureService {

  /**
   * Uploads and sets a new room picture
   *
   * @param roomId      room identifier {@link UUID }
   * @param image       image to set {@link File}
   * @param currentUser current authenticated user {@link MockUserPrincipal}
   **/
  void setPictureForRoom(UUID roomId, File image, MockUserPrincipal currentUser);

}
