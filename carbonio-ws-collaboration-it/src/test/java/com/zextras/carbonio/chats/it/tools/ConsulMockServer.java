// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.ws.rs.HttpMethod;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;

public class ConsulMockServer extends ClientAndServer implements CloseableResource {

  public ConsulMockServer(Integer... ports) {
    super(ports);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping Consul Server mock...");
    super.close();
  }

  /**
   * Stubs the local agent service catalog ({@code GET /v1/agent/services}) so that {@code
   * ServiceIdProvider} can resolve this instance's {@code service_id} from {@code
   * Service.Meta["service_id"]}. The response shape matches {@link
   * com.orbitz.consul.model.health.Service} (required fields: ID, Service, Tags, Address, Meta,
   * Port).
   */
  public void mockServiceIdResponse(UUID serviceId) {
    when(HttpRequest.request().withMethod(HttpMethod.GET).withPath("/v1/agent/services"))
        .respond(
            HttpResponse.response()
                .withBody(
                    JsonBody.json(
                        "{\n"
                            + "    \"carbonio-ws-collaboration\": {\n"
                            + "        \"ID\": \"carbonio-ws-collaboration\",\n"
                            + "        \"Service\": \"carbonio-ws-collaboration\",\n"
                            + "        \"Tags\": [],\n"
                            + "        \"Address\": \"127.0.0.1\",\n"
                            + "        \"Meta\": {\n"
                            + "            \"service_id\": \""
                            + serviceId
                            + "\"\n"
                            + "        },\n"
                            + "        \"Port\": 10000\n"
                            + "    }\n"
                            + "}"))
                .withStatusCode(200));
  }
}
