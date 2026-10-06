// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.zextras.carbonio.videorecorder.sdk.rest.api.JobResourceApi;
import com.zextras.carbonio.videorecorder.sdk.rest.model.ProcessRecordingRequestDto;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VideoRecorderClientTest {

  JobResourceApi api = mock(JobResourceApi.class);
  VideoRecorderClient client = new VideoRecorderClient(api);

  @Test
  void withServerIdSendsTheServiceIdHeader() throws Exception {
    client.process(new RecordingInfo("vs-1", "m1", "Weekly", "u1", "f1"));

    ArgumentCaptor<ProcessRecordingRequestDto> body = ArgumentCaptor.forClass(ProcessRecordingRequestDto.class);
    verify(api).internalRecordingsProcessPost(body.capture(), eq(Map.of("service-id", "vs-1")));
    assertThat(body.getValue().getMeetingId()).isEqualTo("m1");
    assertThat(body.getValue().getMeetingName()).isEqualTo("Weekly");
    assertThat(body.getValue().getAccountId()).isEqualTo("u1");
    assertThat(body.getValue().getFolderId()).isEqualTo("f1");
  }

  @Test
  void stopWithoutServerIdOmitsHeader() throws Exception {
    client.process(new RecordingInfo(null, "m1", "Weekly", "u1", null));
    verify(api).internalRecordingsProcessPost(any(), eq(Map.of()));
  }
}
