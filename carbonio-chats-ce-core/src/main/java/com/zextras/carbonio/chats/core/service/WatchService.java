package com.zextras.carbonio.chats.core.service;

import com.ecwid.consul.v1.kv.model.GetValue;
import java.util.List;

public interface WatchService {

  void setConsulProperties(List<GetValue> consulPropertyDto);

}
