// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.storages.internal.pojo.BulkDeleteItem;
import com.zextras.storages.internal.pojo.Query;
import com.zextras.storages.internal.pojo.StoragesBulkDeleteBody;
import com.zextras.storages.internal.pojo.StoragesBulkDeleteResponse;
import com.zextras.storages.internal.pojo.StoragesUploadResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;
import org.mockserver.verify.VerificationTimes;

public class StorageMockServer extends ClientAndServer implements CloseableResource {

  public StorageMockServer(Integer... ports) {
    super(ports);
  }

  public StorageMockServer(String remoteHost, Integer remotePort, Integer... ports) {
    super(remoteHost, remotePort, ports);
  }

  public void verify(String method, String path, String node, int iterationsNumber) {
    HttpRequest request = request()
      .withMethod(method)
      .withPath(path)
      .withQueryStringParameter("node", node)
      .withQueryStringParameter("type", "chats");

    verify(request, VerificationTimes.exactly(iterationsNumber));
    clear(request, ClearType.LOG);
  }

  public void setIsAliveResponse(boolean success) {
    HttpRequest request = request().withMethod("GET").withPath("/health/live");
    clear(request);
    when(request)
      .respond(
        response()
          .withStatusCode(success ? 204 : 500)
      );
  }


  public HttpRequest getBulkDeleteRequest(List<String> requestIds) {
    StoragesBulkDeleteBody requestBody = new StoragesBulkDeleteBody();
    requestBody.setIds(requestIds.stream().map(fileId -> {
      BulkDeleteItem item = new BulkDeleteItem();
      item.setNode(fileId);
      return item;
    }).collect(Collectors.toList()));
    return request()
      .withMethod("POST")
      .withPath("/bulk-delete")
      .withQueryStringParameter(param("type", "chats"))
      .withBody(JsonBody.json(requestBody));
  }

  public void setBulkDeleteResponse(List<String> requestIds, List<String> responseIds) {
    HttpRequest request = getBulkDeleteRequest(requestIds);
    clear(request);
    if (responseIds != null) {
      StoragesBulkDeleteResponse responseBody = new StoragesBulkDeleteResponse();
      responseBody.setIds(responseIds.stream().map(fileId -> {
        Query query = new Query();
        query.setType("chats");
        query.setNode(fileId);
        return query;
      }).collect(Collectors.toList()));
      when(request).respond(
        response()
          .withStatusCode(200)
          .withBody(JsonBody.json(responseBody)));
    } else {
      when(request).respond(response().withStatusCode(500));
    }
  }

  public HttpRequest getCopyFileRequest(String sourceId, String destinationId) {
    return request()
      .withMethod("PUT")
      .withPath("/copy?")
      .withQueryStringParameters(
        param("sourceNode", sourceId),
        param("sourceVersion", String.valueOf(0)),
        param("destinationNode", destinationId),
        param("destinationVersion", String.valueOf(0)),
        param("type", "files"),
        param("override", "false"));
  }

  public void mockCopyFile(String sourceId, String destinationId, boolean success) {
    StoragesUploadResponse response = new StoragesUploadResponse();
    response.setQuery(new Query());
    response.getQuery().setNode(destinationId);
    response.getQuery().setType("chats");
    HttpRequest request = getCopyFileRequest(sourceId, destinationId);
    clear(request);
    when(request).respond(
      response()
        .withStatusCode(success ? 200 : 500)
        .withBody(JsonBody.json(response)));
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping storages mock...");
    super.close();
  }
}
