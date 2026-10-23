// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

public final class XmlText {

  private XmlText() {}

  public static String stripIllegal(String raw) {
    int length = raw.length();
    int i = 0;
    while (i < length && isLegalAt(raw, i)) {
      i += Character.isHighSurrogate(raw.charAt(i)) ? 2 : 1;
    }
    if (i >= length) {
      return raw;
    }
    StringBuilder out = new StringBuilder(length);
    out.append(raw, 0, i);
    while (i < length) {
      if (isLegalAt(raw, i)) {
        int width = Character.isHighSurrogate(raw.charAt(i)) ? 2 : 1;
        out.append(raw, i, i + width);
        i += width;
      } else {
        i++;
      }
    }
    return out.toString();
  }

  public static String decodeUnicodeEscapes(String s) {
    if (s.indexOf('\\') < 0) {
      return stripIllegal(s);
    }
    int length = s.length();
    StringBuilder out = new StringBuilder(length);
    int i = 0;
    while (i < length) {
      char c = s.charAt(i);
      if (c != '\\' || i + 1 >= length) {
        out.append(c);
        i++;
      } else if (s.charAt(i + 1) == '\\') {
        out.append('\\');
        i += 2;
      } else {
        int unit = s.charAt(i + 1) == 'u' ? hex4(s, i + 2) : -1;
        if (unit >= 0) {
          out.append((char) unit);
          i += 6;
        } else {
          out.append(c);
          i++;
        }
      }
    }
    return stripIllegal(out.toString());
  }

  private static int hex4(String s, int from) {
    if (from + 4 > s.length()) {
      return -1;
    }
    int value = 0;
    for (int k = from; k < from + 4; k++) {
      int digit = hexDigit(s.charAt(k));
      if (digit < 0) {
        return -1;
      }
      value = (value << 4) | digit;
    }
    return value;
  }

  private static int hexDigit(char c) {
    if (c >= '0' && c <= '9') {
      return c - '0';
    }
    if (c >= 'a' && c <= 'f') {
      return c - 'a' + 10;
    }
    if (c >= 'A' && c <= 'F') {
      return c - 'A' + 10;
    }
    return -1;
  }

  private static boolean isLegalAt(String raw, int i) {
    char c = raw.charAt(i);
    if (Character.isHighSurrogate(c)) {
      return i + 1 < raw.length() && Character.isLowSurrogate(raw.charAt(i + 1));
    }
    return isLegalBmp(c);
  }

  private static boolean isLegalBmp(char c) {
    if (Character.isSurrogate(c)) {
      return false;
    }
    if (c < 0x20) {
      return c == 0x9 || c == 0xA || c == 0xD;
    }
    return c != 0xFFFE && c != 0xFFFF;
  }
}
