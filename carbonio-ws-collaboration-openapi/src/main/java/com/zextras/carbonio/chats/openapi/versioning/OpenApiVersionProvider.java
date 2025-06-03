// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.openapi.versioning;

import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class OpenApiVersionProvider {

  private static final String VERSION;

  static {
    VERSION = loadVersionFromYaml();
  }

  private OpenApiVersionProvider() {}

  private static String loadVersionFromYaml() {
    try (InputStream input = OpenApiVersionProvider.class.getResourceAsStream("/api.yaml")) {
      if (input == null) {
        throw new RuntimeException("openapi.yaml not found on classpath");
      }
      Yaml yaml = new Yaml();
      Map<String, Map<String, String>> data = yaml.load(input);
      Map<String, String> info = data.get("info");
      return info.get("version");
    } catch (Exception e) {
      throw new RuntimeException("Failed to load API version from openapi.yaml", e);
    }
  }

  public static String getVersion() {
    return VERSION;
  }
}
