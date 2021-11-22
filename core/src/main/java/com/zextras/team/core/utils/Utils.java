package com.zextras.team.core.utils;

import java.util.Date;
import org.apache.commons.codec.binary.Base32;

public class Utils {
  /**
   * Calculates the hash of a UUID passed. This hash is calculated with the following formula:
   * <pre>ROT13(Base32(UUID + "-" + clock.now()))</pre>
   *
   * The result has its paddings removed since padding can be inferred when decoding from
   * the length of the string modulo 8, thus allowing to have an URL-embeddable hash
   *
   * @param uuidToEncode the passed UUID
   * @return a {@link String} with the computed hash
   */
  public static String encodeUuidHash(final String uuidToEncode)  {
    String t = Long.toString((new Date()).getTime());
    String hashBytes = new Base32().encodeToString((uuidToEncode + "-" + t).getBytes()).replaceAll("=", "");
    StringBuilder encodedHash = new StringBuilder(hashBytes.length());

    for (char ch : hashBytes.toCharArray()) {
      if(ch >= 65 && ch <= 90) {
        encodedHash.append((char) (((ch - 65 + 13) % 26) + 65));
      } else if (ch >= 97 && ch <= 122) {
        encodedHash.append((char) (((ch - 97 + 13) % 26) + 97));
      } else {
        encodedHash.append(ch);
      }
    }

    return encodedHash.toString();
  }
}
