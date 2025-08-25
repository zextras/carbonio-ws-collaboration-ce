// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.openapi.versioning;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class VersionProvider {

  private static final String VERSION;
  private static final List<String> SUPPORTED_VERSIONS;

  static {
    VERSION = loadVersionFromYaml();
    SUPPORTED_VERSIONS = loadSupportedVersions();
  }

  private VersionProvider() {}

  private static String loadVersionFromYaml() {
    try (InputStream input = VersionProvider.class.getResourceAsStream("/api.yaml")) {
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

  private static List<String> loadSupportedVersions() {
    try (InputStream input = VersionProvider.class.getResourceAsStream("/asyncapi.yaml")) {
      if (input == null) {
        throw new RuntimeException("openapi.yaml not found on classpath");
      }
      Yaml yaml = new Yaml();
      Map<String, Map<String, List<String>>> data = yaml.load(input);
      Map<String, List<String>> info = data.get("info");
      return info.get("x-supported-versions");
    } catch (Exception e) {
      throw new RuntimeException("Failed to load API version from openapi.yaml", e);
    }
  }

  public static String getVersion() {
    return VERSION;
  }

  public static List<String> getSupportedVersions() {
    return SUPPORTED_VERSIONS;
  }
}
