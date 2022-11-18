package com.zextras.carbonio.chats.meeting.web.api;

import com.zextras.carbonio.chats.meeting.api.NotFoundException;
import com.zextras.carbonio.chats.meeting.api.TestApiService;
import com.zextras.carbonio.chats.meeting.model.TestDto;
import com.zextras.carbonio.chats.meeting.service.TestService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Singleton
public class TestApiServiceImpl implements TestApiService {

  private final TestService testService;

  @Inject
  public TestApiServiceImpl(TestService testService) {
    this.testService = testService;
  }

  @Override
  public Response getTest(String id, SecurityContext securityContext) throws NotFoundException {
    return Response.ok().entity(testService.getTest(id)).build();
  }
}
