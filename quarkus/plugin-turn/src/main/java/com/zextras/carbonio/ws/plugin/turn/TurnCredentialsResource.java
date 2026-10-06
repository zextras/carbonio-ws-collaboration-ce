// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.turn;

import com.zextras.carbonio.ws.plugin.api.CurrentUser;
import com.zextras.carbonio.ws.plugin.api.ServiceDiscovery;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

@Path("/meetings/{meetingId}/turnCredentials")
@Authenticated
public class TurnCredentialsResource {

  static final String TURN_SERVICE = "carbonio-turn-server-auth";

  @Inject ServiceDiscovery discovery;
  @Inject VideoServerGateway videoServer;
  @Inject TurnServerService turn;
  @Inject CurrentUser currentUser;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(@PathParam("meetingId") UUID meetingId) {
    UserPrincipal user = currentUser.get();
    if (!discovery.isAvailable(TURN_SERVICE)) {
      return Response.noContent().build();
    }
    String serverId = videoServer.roomsOf(meetingId).map(VideoServerRooms::serverId).orElse(null);
    if (serverId == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    try {
      return turn.getCredentials(user.id().toString(), serverId)
          .map(c -> Response.ok(c).build())
          .orElseGet(() -> Response.noContent().build());
    } catch (TurnServerException e) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
}
