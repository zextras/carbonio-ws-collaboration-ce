// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeRequest;

/**
 * This class represents the audio bridge request to join a room.
 *
 * @see <a href=
 *      "https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeJoinRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomConfigureRequest extends AudioBridgeRequest {

    public static final String CONFIGURE = "configure";

    private String request;
    private Boolean restart;

    public static VideoRoomConfigureRequest create() {
        return new VideoRoomConfigureRequest();
    }

    public String getRequest() {
        return request;
    }

    public VideoRoomConfigureRequest request(String request) {
        this.request = request;
        return this;
    }

    public Boolean getRestart() {
        return restart;
    }

    public VideoRoomConfigureRequest restart(boolean restart) {
        this.restart = restart;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        VideoRoomConfigureRequest that = (VideoRoomConfigureRequest) o;
        return Objects.equals(request, that.request) && Objects.equals(restart, that.restart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request, restart);
    }
}