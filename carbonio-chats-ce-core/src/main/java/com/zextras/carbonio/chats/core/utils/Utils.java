// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.utils;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.codec.binary.Base32;

public class Utils {

  /**
   * Calculates the hash of a UUID passed. This hash is calculated with the following formula:
   * <pre>ROT13(Base32(UUID + "-" + clock.now()))</pre>
   * <p>
   * The result has its paddings removed since padding can be inferred when decoding from the length of the string
   * modulo 8, thus allowing to have an URL-embeddable hash
   *
   * @param uuidToEncode the passed UUID
   * @return a {@link String} with the computed hash
   */
  public static String encodeUuidHash(final String uuidToEncode, Clock clock) {
    String t = Long.toString(OffsetDateTime.ofInstant(clock.instant(), clock.getZone()).toEpochSecond());
    String hashBytes = new Base32().encodeToString((uuidToEncode + "-" + t).getBytes()).replaceAll("=", "");
    StringBuilder encodedHash = new StringBuilder(hashBytes.length());

    for (char ch : hashBytes.toCharArray()) {
      if (ch >= 65 && ch <= 90) {
        encodedHash.append((char) (((ch - 65 + 13) % 26) + 65));
      } else if (ch >= 97 && ch <= 122) {
        encodedHash.append((char) (((ch - 97 + 13) % 26) + 97));
      } else {
        encodedHash.append(ch);
      }
    }

    return encodedHash.toString();
  }

  public static Optional<String> getFilePropertyFromContentDisposition(String xContentDisposition, String property) {
    if (xContentDisposition.contains(property)) {
      String value = xContentDisposition.substring(xContentDisposition.indexOf(property) + property.length() + 1);
      if (value.contains(";")) {
        value = value.substring(0, value.indexOf(";"));
      }
      value = value.trim();
      return Optional.of("fileName".equals(property) ? new String(Base64.getDecoder().decode(value)) : value);
    } else {
      return Optional.empty();
    }
  }
}
