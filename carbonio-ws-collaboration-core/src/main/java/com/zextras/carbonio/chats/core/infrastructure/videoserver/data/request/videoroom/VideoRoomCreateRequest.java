// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the video room request to create a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomCreateRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomCreateRequest extends VideoRoomRequest {

  public static final String CREATE = "create";
  public static final String ROOM_DEFAULT = "video_";
  public static final String DESCRIPTION_DEFAULT = "video_room_";
  public static final int MAX_PUBLISHERS_DEFAULT = 100;
  public static final long BITRATE_DEFAULT = 400L * 1024;

  private String request;
  private String room;
  private Boolean permanent;
  private String description;
  private Boolean isPrivate;

  private Integer publishers;
  private Long bitrate;
  private Boolean bitrateCap;

  @JsonProperty("videocodec")
  private String videoCodec;

  private Boolean record;

  public static VideoRoomCreateRequest create() {
    return new VideoRoomCreateRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomCreateRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomCreateRequest room(String room) {
    this.room = room;
    return this;
  }

  public Boolean getPermanent() {
    return permanent;
  }

  public VideoRoomCreateRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public VideoRoomCreateRequest description(String description) {
    this.description = description;
    return this;
  }

  public Boolean getIsPrivate() {
    return isPrivate;
  }

  public VideoRoomCreateRequest isPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
    return this;
  }

  public Integer getPublishers() {
    return publishers;
  }

  public VideoRoomCreateRequest publishers(int publishers) {
    this.publishers = publishers;
    return this;
  }

  public Long getBitrate() {
    return bitrate;
  }

  public VideoRoomCreateRequest bitrate(long bitrate) {
    this.bitrate = bitrate;
    return this;
  }

  public Boolean getBitrateCap() {
    return bitrateCap;
  }

  public VideoRoomCreateRequest bitrateCap(boolean bitrateCap) {
    this.bitrateCap = bitrateCap;
    return this;
  }

  public String getVideoCodec() {
    return videoCodec;
  }

  public VideoRoomCreateRequest videoCodec(String videoCodec) {
    this.videoCodec = videoCodec;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public VideoRoomCreateRequest record(boolean record) {
    this.record = record;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomCreateRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getPermanent(), that.getPermanent())
        && Objects.equals(getDescription(), that.getDescription())
        && Objects.equals(getIsPrivate(), that.getIsPrivate())
        && Objects.equals(getPublishers(), that.getPublishers())
        && Objects.equals(getBitrate(), that.getBitrate())
        && Objects.equals(getBitrateCap(), that.getBitrateCap())
        && Objects.equals(getVideoCodec(), that.getVideoCodec())
        && Objects.equals(getRecord(), that.getRecord());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getRequest(),
        getRoom(),
        getPermanent(),
        getDescription(),
        getIsPrivate(),
        getPublishers(),
        getBitrate(),
        getBitrateCap(),
        getVideoCodec(),
        getRecord());
  }
}
