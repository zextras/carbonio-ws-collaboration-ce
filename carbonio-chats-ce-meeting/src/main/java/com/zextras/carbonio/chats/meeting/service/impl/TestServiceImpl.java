package com.zextras.carbonio.chats.meeting.service.impl;

import com.zextras.carbonio.chats.meeting.model.TestDto;
import com.zextras.carbonio.chats.meeting.service.TestService;
import javax.inject.Singleton;

@Singleton
public class TestServiceImpl implements TestService {

  @Override
  public TestDto getTest(String id) {
    return TestDto.create().test("TEST").id(id);
  }
}
