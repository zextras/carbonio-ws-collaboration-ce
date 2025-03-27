// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.utility;

import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpClient {

  private static final CloseableHttpClient client = HttpClientProvider.getHttpClient();

  public CloseableHttpResponse sendPost(String url, Map<String, String> headers, String body)
      throws IOException {
    HttpPost request = new HttpPost(url);
    headers.forEach(request::addHeader);
    request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    try {
      return client.execute(request);
    } catch (IOException e) {
      throw new InternalErrorException(e);
    }
  }

  public CloseableHttpResponse sendGet(String url, Map<String, String> headers) throws IOException {
    HttpGet request = new HttpGet(url);
    headers.forEach(request::addHeader);
    try {
      return client.execute(request);
    } catch (IOException e) {
      throw new InternalErrorException(e);
    }
  }
}
