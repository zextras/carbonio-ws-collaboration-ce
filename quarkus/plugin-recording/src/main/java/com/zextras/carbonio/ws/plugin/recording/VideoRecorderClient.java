// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.zextras.carbonio.videorecorder.sdk.rest.ApiException;
import com.zextras.carbonio.videorecorder.sdk.rest.api.JobResourceApi;
import com.zextras.carbonio.videorecorder.sdk.rest.model.ProcessRecordingRequestDto;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;

@ApplicationScoped
public class VideoRecorderClient {

  private static final String SERVICE_ID_HEADER = "service-id";

  private final JobResourceApi api;

  @Inject
  public VideoRecorderClient(JobResourceApi api) {
    this.api = api;
  }

  /** Routed to the videorecorder co-located with the videoserver that holds the files. */
  public void process(RecordingInfo info) {
    ProcessRecordingRequestDto request =
        new ProcessRecordingRequestDto()
            .meetingId(info.meetingId())
            .meetingName(info.meetingName())
            .accountId(info.accountId())
            .folderId(info.folderId());
    Map<String, String> headers =
        info.serverId() == null ? Map.of() : Map.of(SERVICE_ID_HEADER, info.serverId());
    try {
      api.internalRecordingsProcessPost(request, headers);
    } catch (ApiException e) {
      throw new IllegalStateException(
          "Failed to send video recorder request for meeting " + info.meetingId(), e);
    }
  }
}
