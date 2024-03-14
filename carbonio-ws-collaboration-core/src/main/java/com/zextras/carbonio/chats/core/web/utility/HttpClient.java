// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.utility;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

@Singleton
public class HttpClient {

  public CloseableHttpResponse sendPost(String url, Map<String, String> headers, String body)
      throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    try {
      return httpClient.execute(getHttpPost(url, headers, body));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private HttpPost getHttpPost(String url, Map<String, String> header, String body) {
    HttpPost request = new HttpPost(url);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    header.forEach(request::addHeader);
    request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    return request;
  }

  public CloseableHttpResponse sendGet(String url, Map<String, String> headers) throws IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    try {
      return httpClient.execute(getHttpGet(url, headers));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private HttpGet getHttpGet(String url, Map<String, String> headers) {
    HttpGet request = new HttpGet(url);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    headers.forEach(request::addHeader);
    return request;
  }
}
