// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import javax.inject.Singleton;

@Singleton
public class VideoServerServiceMock implements VideoServerService {

  @Override
  public void startMeeting(String meetingId) {
    //Mock class with empty method
  }

  @Override
  public void stopMeeting(String meetingId) {
    //Mock class with empty method
  }

  @Override
  public void joinMeeting(String userId, String queueId, String meetingId, boolean videoStreamOn, boolean audioStreamOn) {

  }

  @Override
  public void leaveMeeting(String sessionId, String meetingId) {

  }

  @Override
  public void updateAudioStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void updateMediaStream(String sessionId, String meetingId, MediaStreamSettingsDto videoStreamSettingsDto) {

  }

  @Override
  public void answerRtcMediaStream(String sessionId, String meetingId, String sdp) {

  }

  @Override
  public void updateSubscriptionsMediaStream(String sessionId, String meetingId,
    SubscriptionUpdatesDto subscriptionUpdatesDto) {

  }

  @Override
  public void offerRtcAudioStream(String sessionId, String meetingId, String sdp) {

  }

  @Override
  public boolean isAlive() {
    return true;
  }
}
