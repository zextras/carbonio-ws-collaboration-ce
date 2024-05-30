// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.utils;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.apache.commons.text.StringEscapeUtils;

public class StringFormatUtils {

  public static String encodeToUtf8(String toEncode) {
    if (toEncode == null || toEncode.length() == 0) {
      return toEncode;
    }
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < toEncode.length(); i++) {
      try {
        
        result.append(encodeSingleCharToUtf8(toEncode.substring(i, i + 1)));
      } catch (UnsupportedEncodingException ignored) {
      }
    }
    return result.toString();
  }

  private static String encodeSingleCharToUtf8(String toEncode) throws UnsupportedEncodingException {
    if (toEncode == null || toEncode.length() != 1) {
      throw new UnsupportedEncodingException();
    }
    byte[] bytes = toEncode.getBytes(StandardCharsets.UTF_8);
    ArrayList<String> binaryList = new ArrayList<>();
    for (byte b : bytes) {
      binaryList.add(org.apache.commons.lang3.StringUtils.leftPad(Integer.toBinaryString(b & 0xFF), 8, "0"));
    }
    String filter = org.apache.commons.lang3.StringUtils.leftPad("0", binaryList.size() > 1 ? binaryList.size() + 1 : 0,
      "1");

    StringBuilder binary = new StringBuilder();
    for (String b : binaryList) {
      if (!b.startsWith(filter)) {
        ChatsLogger.warn(StringFormatUtils.class, String.format("Connot encode character '%s'", toEncode));
        throw new UnsupportedEncodingException(String.format("Connot encode character '%s'", toEncode));
      }
      binary.append(b.substring(filter.length()));
      filter = "10";
    }
    return String.format("\\u%4s", Integer.toHexString(Integer.parseInt(binary.toString(), 2)))
      .replace(' ', '0');
  }

  public static String decodeFromUtf8(String toDecode) throws UnsupportedEncodingException {
    if (!isEncodedInUtf8(toDecode)) {
      throw new UnsupportedEncodingException(String.format("'%s' is not encoded to UTF-8 format", toDecode));
    }
    return StringEscapeUtils.unescapeJava(toDecode);
  }

  public static boolean isEncodedInUtf8(String text) {
    return Pattern.matches(ChatsConstant.IS_UNICODE_FORMAT_REGEX, text);
  }
}
