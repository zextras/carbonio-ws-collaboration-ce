// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.zextras.carbonio.ws.plugin.api.CurrentUser;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import java.util.UUID;

@Path("/meetings/{meetingId}")
@Authenticated
public class RecordingResource {

  static final String RECORDING_ENABLED = "carbonioWscRecordingEnabled";

  public record RecordingFields(String name, String folderId) {}

  @Inject RecordingService recordings;
  @Inject CurrentUser currentUser;

  @POST
  @Path("/startRecording")
  public void start(@PathParam("meetingId") UUID meetingId, RecordingFields fields) {
    UserPrincipal user = currentUser.get();
    if (user.isGuest()) {
      throw new ForbiddenException();
    }
    if (!user.hasAttribute(RECORDING_ENABLED)) {
      throw new ForbiddenException("Recording is not enabled for user");
    }
    recordings.start(meetingId, fields == null ? null : fields.folderId(), user);
  }

  @POST
  @Path("/stopRecording")
  public void stop(@PathParam("meetingId") UUID meetingId, RecordingFields fields) {
    UserPrincipal user = currentUser.get();
    if (user.isGuest()) {
      throw new ForbiddenException();
    }
    recordings.stop(meetingId, user);
  }
}
