// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.meeting.model.RtcSessionDescriptionDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import javax.inject.Singleton;

@Singleton
public class VideoServerServiceMock implements VideoServerService {

  @Override
  public void createMeeting(String meetingId) {

  }

  @Override
  public void deleteMeeting(String meetingId) {

  }

  @Override
  public void joinMeeting(String sessionId, String meetingId, boolean videoStreamOn, boolean audioStreamOn) {

  }

  @Override
  public void leaveMeeting(String sessionId, String meetingId) {

  }

  @Override
  public void updateAudioStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void updateVideoStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void updateScreenStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void offerRtcVideoStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {

  }

  @Override
  public void answerRtcMediaStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {

  }

  @Override
  public void updateSubscriptionsMediaStream(String sessionId, String meetingId,
    SubscriptionUpdatesDto subscriptionUpdatesDto) {

  }

  @Override
  public void offerRtcAudioStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {

  }

  @Override
  public void offerRtcScreenStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {

  }

  @Override
  public boolean isAlive() {
    return true;
  }
}
