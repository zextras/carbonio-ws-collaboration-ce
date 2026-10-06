// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.api;

/** Version of the plugin contract exposed by this core-api build. */
public final class PluginApi {

  public static final String VERSION = "1.0";

  private PluginApi() {}

  /**
   * A plugin requiring {@code required} runs on a core providing {@code provided} when the majors
   * are equal and the required minor is not newer than the provided one.
   */
  public static boolean isCompatible(String required, String provided) {
    int[] r = parse(required);
    int[] p = parse(provided);
    return r[0] == p[0] && r[1] <= p[1];
  }

  private static int[] parse(String version) {
    String[] parts = version == null ? new String[0] : version.split("\\.");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid plugin API version: " + version);
    }
    try {
      return new int[] {Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid plugin API version: " + version, e);
    }
  }
}
