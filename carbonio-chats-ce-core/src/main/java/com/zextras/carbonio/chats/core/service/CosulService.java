package com.zextras.carbonio.chats.core.service;

import com.zextras.carbonio.chats.model.ConsulPropertyDto;
import java.util.List;

public interface CosulService {

  /**
   * Inserts or Updates consul configurations list
   *
   * @param consulPropertyDto consul configurations list
   */
  void setConsulProperties(List<ConsulPropertyDto> consulPropertyDto);

}
