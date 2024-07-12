// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.HttpMethod;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

public class ConsulMockServer extends ClientAndServer implements CloseableResource {

  public ConsulMockServer(Integer... ports) {
    super(ports);
  }

  public void verify(
      String method,
      String path,
      @Nullable Map<String, String> queryParameters,
      int iterationsNumber) {
    HttpRequest request = request().withMethod(method).withPath(path);
    Optional.ofNullable(queryParameters)
        .ifPresent(parameters -> parameters.forEach(request::withQueryStringParameter));
    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping Consul Server mock...");
    super.close();
  }

  public void mockConsulLeaderResponse() {
    when(HttpRequest.request().withMethod(HttpMethod.GET).withPath("/v1/status/leader"))
        .respond(HttpResponse.response().withStatusCode(200));
  }

  public void mockResponseForVideoserver(UUID serverId) {
    when(HttpRequest.request()
            .withMethod(HttpMethod.GET)
            .withPath("/v1/health/service/carbonio-videoserver")
            .withQueryStringParameter("passing", "true"))
        .respond(
            HttpResponse.response()
                .withBody(
                    JsonBody.json(
                        "[\n"
                            + "    {\n"
                            + "        \"Node\": {\n"
                            + "            \"ID\": \"6cb11082-d1ab-52d3-ecc9-ee3f82a3fc2d\",\n"
                            + "            \"Node\": \"test-node\",\n"
                            + "            \"Address\": \"172.27.27.207\",\n"
                            + "            \"Datacenter\": \"dc\",\n"
                            + "            \"TaggedAddresses\": {\n"
                            + "                \"lan\": \"172.27.27.207\",\n"
                            + "                \"lan_ipv4\": \"172.27.27.207\",\n"
                            + "                \"wan\": \"172.27.27.207\",\n"
                            + "                \"wan_ipv4\": \"172.27.27.207\"\n"
                            + "            },\n"
                            + "            \"Meta\": {\n"
                            + "                \"consul-network-segment\": \"\"\n"
                            + "            },\n"
                            + "            \"CreateIndex\": 224570,\n"
                            + "            \"ModifyIndex\": 224573\n"
                            + "        },\n"
                            + "        \"Service\": {\n"
                            + "            \"ID\": \"carbonio-videoserver\",\n"
                            + "            \"Service\": \"carbonio-videoserver\",\n"
                            + "            \"Tags\": [],\n"
                            + "            \"Address\": \"\",\n"
                            + "            \"Meta\": {\n"
                            + "                \"service_id\": \""
                            + serverId
                            + "\"\n"
                            + "            },\n"
                            + "            \"Port\": 10000,\n"
                            + "            \"Weights\": {\n"
                            + "                \"Passing\": 1,\n"
                            + "                \"Warning\": 1\n"
                            + "            },\n"
                            + "            \"EnableTagOverride\": false,\n"
                            + "            \"Proxy\": {\n"
                            + "                \"Mode\": \"\",\n"
                            + "                \"MeshGateway\": {},\n"
                            + "                \"Expose\": {}\n"
                            + "            },\n"
                            + "            \"Connect\": {},\n"
                            + "            \"CreateIndex\": 224584,\n"
                            + "            \"ModifyIndex\": 224944\n"
                            + "        },\n"
                            + "        \"Checks\": [\n"
                            + "            {\n"
                            + "                \"Node\": \"test-node\",\n"
                            + "                \"CheckID\": \"serfHealth\",\n"
                            + "                \"Name\": \"Serf Health Status\",\n"
                            + "                \"Status\": \"passing\",\n"
                            + "                \"Notes\": \"\",\n"
                            + "                \"Output\": \"Agent alive and reachable\",\n"
                            + "                \"ServiceID\": \"\",\n"
                            + "                \"ServiceName\": \"\",\n"
                            + "                \"ServiceTags\": [],\n"
                            + "                \"Type\": \"\",\n"
                            + "                \"Interval\": \"\",\n"
                            + "                \"Timeout\": \"\",\n"
                            + "                \"ExposedPort\": 0,\n"
                            + "                \"Definition\": {},\n"
                            + "                \"CreateIndex\": 224570,\n"
                            + "                \"ModifyIndex\": 483600\n"
                            + "            }\n"
                            + "        ]\n"
                            + "    }\n"
                            + "]"))
                .withStatusCode(200));
  }
}
