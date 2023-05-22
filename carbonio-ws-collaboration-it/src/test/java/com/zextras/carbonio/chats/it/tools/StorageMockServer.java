// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.tools;

import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.filestore.powerstore.api.powerstore.BulkDeleteBody;
import com.zextras.filestore.powerstore.api.powerstore.BulkDeleteItem;
import com.zextras.filestore.powerstore.api.powerstore.StoragesBulkDeleteResponse;
import com.zextras.filestore.powerstore.internal.powerstore.PowerstoreUploadResponse;
import com.zextras.filestore.powerstore.internal.powerstore.Query;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.Header;
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

  public HttpRequest getNSLookupUrlRequest(String userId) {
    return request()
      .withMethod("GET")
      .withPath("/service/extension/nginx-lookup")
      .withHeaders(
        Header.header("Auth-Method", "zimbraId"),
        Header.header("Auth-User", userId),
        Header.header("Auth-Protocol", "http"),
        Header.header("X-Proxy-IP", "127.0.0.1"));
  }

  public void mockNSLookupUrl(String userId, boolean success) {
    HttpRequest request = getNSLookupUrlRequest(userId);
    clear(request);
    when(request).respond(
      response()
        .withStatusCode(success ? 200 : 500)
        .withHeaders(
          Header.header("Auth-Server", "127.0.0.1:8742")));
  }

  public HttpRequest getDownloadRequest(String fileId, String userId) {
    return request()
      .withMethod("GET")
      .withPath("/zx/powerstore/v1/download")
      .withQueryStringParameter(param("node", fileId))
      .withQueryStringParameter(param("accountId", userId))
      .withQueryStringParameter(param("type", "chats"));
  }

  public void mockDownload(String fileId, String userId, FileMock fileMock, boolean success) throws IOException {
    HttpRequest request = getDownloadRequest(fileId, userId);
    clear(request);
    when(request).respond(
      response()
        .withStatusCode(success ? 200 : 500)
        .withBody(success ? binary(fileMock.getFileBytes()) : null)
        .withHeaders(
          Header.header("content-type", "application/octet-stream"),
          Header.header("content-disposition", "attachment; filename*=UTF-8''54614d1d-1513-4b21-a606-c45b4b226319"),
          Header.header("content-encoding", "gzip"),
          Header.header("transfer-encoding", "chunked")
        ));
  }

  public HttpRequest getUploadPutRequest(String fileId, String userId) {
    return request()
      .withMethod("PUT")
      .withPath("/zx/powerstore/v1/upload")
      .withQueryStringParameter(param("node", fileId))
      .withQueryStringParameter(param("accountId", userId))
      .withQueryStringParameter(param("type", "chats"));
  }

  public void mockUploadPut(String fileId, String userId, boolean success) {
    PowerstoreUploadResponse responseBody = new PowerstoreUploadResponse();
    responseBody.setDigestAlgorithm("digest-algorithm");
    responseBody.setDigest("digest");
    responseBody.setSize(0L);
    HttpRequest request = getUploadPutRequest(fileId, userId);
    clear(request);
    when(request).respond(
      response()
        .withStatusCode(success ? 200 : 500)
        .withBody(JsonBody.json(responseBody)));
  }

  public HttpRequest getDeleteRequest(String fileId, String userId) {
    return request()
      .withMethod("DELETE")
      .withPath("/zx/powerstore/v1/delete")
      .withQueryStringParameter(param("node", fileId))
      .withQueryStringParameter(param("accountId", userId))
      .withQueryStringParameter(param("type", "chats"));
  }

  public void mockDelete(String fileId, String userId, boolean success) {
    HttpRequest request = getDeleteRequest(fileId, userId);
    clear(request);
    when(request).respond(
      response()
        .withStatusCode(success ? 200 : 500));
  }

  public HttpRequest getBulkDeleteRequest(List<String> requestIds, String userId) {
    BulkDeleteBody requestBody = new BulkDeleteBody();
    requestBody.setIds(requestIds.stream().map(fileId -> {
      BulkDeleteItem item = new BulkDeleteItem();
      item.setNode(fileId);
      return item;
    }).collect(Collectors.toList()));
    return request()
      .withMethod("POST")
      .withPath("/zx/powerstore/v1/bulk-delete")
      .withQueryStringParameter(param("accountId", userId))
      .withQueryStringParameter(param("type", "chats"))
      .withBody(JsonBody.json(requestBody));
  }

  public void mockBulkDelete(List<String> requestIds, String userId, List<String> responseIds, boolean success) {
    HttpRequest request = getBulkDeleteRequest(requestIds, userId);
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
          .withStatusCode(success ? 200 : 500)
          .withBody(success ? JsonBody.json(responseBody) : null));
    } else {
      when(request).respond(response().withStatusCode(500));
    }
  }

  public HttpRequest getCopyFileRequest(
    String sourceId, String sourceUserId, String destinationId, String destinationUserId
  ) {
    return request()
      .withMethod("PUT")
      .withPath("/zx/powerstore/v1/copy")
      .withQueryStringParameters(
        param("accountId", sourceUserId),
        param("sourceNode", sourceId),
        param("destinationAccountId", destinationUserId),
        param("destinationNode", destinationId),
        param("type", "chats"),
        param("override", "false"));
  }

  public void mockCopyFile(
    String sourceId, String sourceUserId, String destinationId, String destinationUserId, boolean success
  ) {
    PowerstoreUploadResponse responseBody = new PowerstoreUploadResponse();
    responseBody.setDigestAlgorithm("digest-algorithm");
    responseBody.setDigest("digest");
    responseBody.setSize(0L);
    HttpRequest request = getCopyFileRequest(sourceId, sourceUserId, destinationId, destinationUserId);
    clear(request);
    when(request).respond(
      response()
        .withStatusCode(success ? 200 : 500)
        .withBody(success ? JsonBody.json(responseBody) : null));
  }

  @Override
  public void close() {
    ChatsLogger.debug("Stopping storages mock...");
    super.close();
  }
}
