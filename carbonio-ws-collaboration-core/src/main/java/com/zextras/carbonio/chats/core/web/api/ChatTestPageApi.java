// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Serves the chat test page for WebSocket testing. Assembles HTML from separate CSS, HTML body, and
 * JS files in the classpath for easier maintenance.
 */
@Path("/new-chat")
public class ChatTestPageApi {

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Response getTestPage() {
    try {
      String css = loadResource("new-chat/style.css");
      String body = loadResource("new-chat/body.html");
      String js = loadResource("new-chat/app.js");

      String html =
          "<!DOCTYPE html>\n"
              + "<html lang=\"en\">\n"
              + "<head>\n"
              + "  <meta charset=\"UTF-8\">\n"
              + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
              + "  <title>Chats</title>\n"
              + "  <style>\n"
              + css
              + "\n  </style>\n"
              + "</head>\n"
              + "<body>\n"
              + body
              + "\n<script>\n"
              + js
              + "\n</script>\n"
              + "</body>\n"
              + "</html>";

      return Response.ok(html).build();
    } catch (Exception e) {
      return Response.serverError()
          .entity("Error loading page: " + e.getMessage())
          .build();
    }
  }

  private String loadResource(String path) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
      if (is == null) {
        throw new RuntimeException("Resource not found: " + path);
      }
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        return reader.lines().collect(Collectors.joining("\n"));
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load resource: " + path, e);
    }
  }
}
