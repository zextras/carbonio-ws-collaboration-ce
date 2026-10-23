// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class XmlTextTest {

  @Test
  @DisplayName("Drops C0 controls, NUL, U+FFFE and U+FFFF leaving the neighbours")
  void dropsIllegalCharacters() {
    assertEquals("loremi  psumd", XmlText.stripIllegal("loremi \u0001 psumd"));
    assertEquals("loremi  psumd", XmlText.stripIllegal("loremi \u0000 psumd"));
    assertEquals("loremi  psumd", XmlText.stripIllegal("loremi \u001f psumd"));
    assertEquals("loremi  psumd", XmlText.stripIllegal("loremi \uFFFE psumd"));
    assertEquals("lor  emip", XmlText.stripIllegal("lor \uFFFF emip"));
  }

  @Test
  @DisplayName("Keeps legal unusual characters")
  void keepsLegalUnusualCharacters() {
    String legal =
        "\t\n\r\u007f\u0080\u009f\uFEFF\uD83F\uDFFE\uDBFF\uDFFF\uE000\uFFFD\u202e\u200d\u200b\u0301"
            + "\u2028\u2029\u0085";
    assertEquals(legal, XmlText.stripIllegal(legal));
  }

  @Test
  @DisplayName("Drops unpaired surrogates and keeps pairs")
  void unpairedSurrogates() {
    assertEquals("a\uD83D\uDE00b", XmlText.stripIllegal("a\uD83D\uDE00b"));
    assertEquals("ab", XmlText.stripIllegal("a\uD83Db"));
    assertEquals("ab", XmlText.stripIllegal("a\uDE00b"));
    assertEquals("a", XmlText.stripIllegal("a\uD83D"));
  }

  @Test
  @DisplayName("Decodes escapes, combines surrogates, case-insensitive hex")
  void decodesEscapes() {
    assertEquals(
        "lorem ipsu \u00e9 \uD83D\uDE00",
        XmlText.decodeUnicodeEscapes(
            "\\u006c\\u006f\\u0072\\u0065\\u006d\\u0020\\u0069\\u0070\\u0073\\u0075\\u0020\\u00E9"
                + "\\u0020\\ud83d\\uDE00"));
    assertEquals(
        "\uD83D\uDE00\uD83D\uDE80", XmlText.decodeUnicodeEscapes("\\ud83d\\ude00\\uD83D\\uDE80"));
  }

  @Test
  @DisplayName("An escaped backslash is not rescanned")
  void escapedBackslash() {
    assertEquals(
        "L:\\orem\\u0041 lor \\", XmlText.decodeUnicodeEscapes("L:\\\\orem\\\\u0041 lor \\\\"));
    assertEquals("lor \\\\u0041", XmlText.decodeUnicodeEscapes("lor \\\\\\\\u0041"));
    assertEquals("\\", XmlText.decodeUnicodeEscapes("\\"));
    assertEquals("\\e \\s \\u00", XmlText.decodeUnicodeEscapes("\\e \\s \\u00"));
  }

  @Test
  @DisplayName("Five hex digits decode four and keep the fifth")
  void fiveHexDigits() {
    assertEquals(
        "lor emip2 \u1f60" + "0 \u00e9",
        XmlText.decodeUnicodeEscapes("lor emip2 \\u1f600 \\u00e9"));
  }

  @Test
  @DisplayName("Escaped controls are dropped, DEL is kept, markup lookalikes stay literal")
  void escapedControls() {
    assertEquals(
        "lore mip-sum dol\u007f ors",
        XmlText.decodeUnicodeEscapes("lore \\u0001mip-sum dol\\u007f \\u0000ors"));
    assertEquals(
        "]]> <![LOREM[i]]> &lt; &#x1F600; </lore>",
        XmlText.decodeUnicodeEscapes("]]> <![LOREM[i]]> &lt; &#x1F600; </lore>"));
    assertEquals("a", XmlText.decodeUnicodeEscapes("a\\ud83d"));
  }
}
