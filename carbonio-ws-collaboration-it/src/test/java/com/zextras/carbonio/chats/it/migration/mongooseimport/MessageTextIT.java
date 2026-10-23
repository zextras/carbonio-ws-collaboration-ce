// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@DisplayName("V2_0_1 import of the real MongooseIM dump: message text")
class MessageTextIT {

  private static final String EDGE_ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String GROUP_A_ROOM = "132fe05c-2df1-4ce8-9738-85188aca781b";
  private static final String DIRECT_A_ROOM = "fa0dfad7-7583-4b95-9bd4-a38e7431f1d9";
  private static final String DIRECT_B_ROOM = "61a47ae3-8c95-47f5-8ce8-2b8c22b46402";
  private static final String GROUP_B_ROOM = "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";
  private static final String GROUP_C_ROOM = "d39be5dc-cb00-4ad4-9ae3-03f4f0894b99";
  private static final String GROUP_D_ROOM = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String GROUP_E_ROOM = "7e7d6c0a-efce-4852-b197-8bf33d267290";

  private static final long HYPHEN_COMMA_PUNCTUATION_MAM = 458596131433541633L;
  private static final long MIXED_CASE_SHARP_S_MAM = 458595995663630337L;
  private static final long WINDOWS_PATH_BACKSLASHES_MAM = 458596034203844865L;
  private static final long URL_WITH_SCHEME_MAM = 458595988866599937L;
  private static final long CODE_FENCE_WITH_QUOTES_MAM = 458595995437291009L;
  private static final long CDATA_END_MARKER_MAM = 458596044013108481L;
  private static final long EMOJI_ONLY_GRIN_MAM = 458595958524937217L;
  private static final long EMOJI_ONLY_THUMBS_UP_MAM = 458595994540675585L;
  private static final long EMOJI_ONLY_THINKING_MAM = 458595994377722881L;
  private static final long LEADING_AND_TRAILING_SPACES_MAM = 458596928959606529L;
  private static final long TRAILING_SPACES_ONLY_MAM = 458596047663455233L;
  private static final long TRAILING_SPACE_AFTER_MARKUP_MAM = 458596929768624385L;
  private static final long THREE_BODIES_MAM = 458596928458483713L;
  private static final long BODY_LANG_IT_MAM = 458596928559474689L;
  private static final long BODY_LANG_EMPTY_MAM = 458596928658711553L;
  private static final long BODY_FOREIGN_ATTRIBUTE_MAM = 458596933169913345L;
  private static final long BODY_THEN_XHTML_BODY_MAM = 458596931982350081L;
  private static final long BODY_WITH_BOLD_CHILD_MAM = 458596928859154689L;
  private static final long IOS_REPLY_BEFORE_BODY_MAM = 458596912617206529L;
  private static final long GROUP_REPLY_BEFORE_BODY_MAM = 458596166505477633L;
  private static final long SMALL_BODY_HUGE_STANZA_MAM = 458596998009872641L;
  private static final long EMOJI_AMP_AND_ANGLE_BRACKETS_MAM = 458596130952850177L;
  private static final long QUOTES_AND_BACKTICKS_MAM = 458596929569362433L;
  private static final long ENTITIES_ONLY_AMP_LT_GT_MAM = 458595988258244097L;
  private static final long AMP_THEN_QUOTES_ENTITY_MAM = 458596929668990721L;
  private static final long AMP_L_SEMICOLON_MAM = 458596973767546881L;
  private static final long AMP_NUMERIC_REFERENCE_MAM = 458595985868840193L;
  private static final long AMP_NAMED_REFERENCE_MAM = 458596954488700673L;
  private static final long ESCAPED_TAG_PAIR_MAM = 458596035803636225L;
  private static final long ESCAPED_COMPARISON_MAM = 458596046825958145L;
  private static final long ESCAPED_TAGS_AND_BRACES_MAM = 458596931780249601L;
  private static final long ESCAPED_TAG_INLINE_MAM = 458596929871074305L;
  private static final long ACCENT_AND_THREE_FACES_MAM = 458595991612102913L;
  private static final long PRAYER_HANDS_AFTER_LETTERS_MAM = 458596030282127617L;
  private static final long FAMILY_THUMBS_FLAG_KEYCAP_MAM = 458596930770387201L;
  private static final long LETTER_AND_FLAG_MAM = 458596033145695233L;
  private static final long TECHNOLOGIST_ZWJ_SEQUENCE_MAM = 458596034793768449L;
  private static final long RED_HEART_VARIATION_SELECTOR_MAM = 458596043286163457L;
  private static final long HEBREW_MAM = 458596025667298305L;
  private static final long CYRILLIC_QUESTION_MAM = 458596026288026881L;
  private static final long CYRILLIC_SENTENCE_MAM = 458596039125281025L;
  private static final long JAPANESE_MAM = 458596027536249345L;
  private static final long MIXED_CJK_ARABIC_HEBREW_THAI_MAM = 458596931274098689L;
  private static final long COMBINING_MARKS_MAM = 458596931072003585L;
  private static final long BIDI_OVERRIDES_MAM = 458596930870058241L;
  private static final long ZERO_WIDTH_CHARACTERS_MAM = 458596930669489665L;
  private static final long QUOTED_THREE_LINES_MAM = 458595958910567681L;
  private static final long BLANK_LINES_BETWEEN_MAM = 458596045350057985L;
  private static final long LEADING_NEWLINE_MAM = 458596029643491841L;
  private static final long TRAILING_NEWLINE_MAM = 458595991315875841L;
  private static final long CODE_FENCE_TWO_LINES_MAM = 458596042759593985L;
  private static final long LIST_DASHES_AND_INDENT_MAM = 458596036758256897L;
  private static final long CRLF_AND_LONE_CR_MAM = 458596929060383233L;
  private static final long SINGLE_CRLF_MAM = 458596929160340737L;
  private static final long TAB_LF_CR_MAM = 458596931477356289L;
  private static final long UNICODE_LINE_SEPARATORS_MAM = 458596930971051777L;
  private static final long TYPED_U00E8_AND_U0041_MAM = 458596929265351425L;
  private static final long TYPED_SURROGATE_PAIR_ESCAPES_MAM = 458596929367382785L;
  private static final long TYPED_U00E9_IN_DIRECT_ROOM_MAM = 458595987467621121L;
  private static final long TYPED_U00E9_SECOND_ROW_MAM = 458595990349140481L;
  private static final long TYPED_U00E8_IN_OTHER_GROUP_MAM = 458615900362469889L;
  private static final long BACKSLASH_E_S_AND_DOUBLE_BACKSLASH_U_MAM = 458596929469681153L;
  private static final long LEADING_BACKSLASH_L_MAM = 458596037835991041L;
  private static final long DEL_AND_C1_CONTROLS_MAM = 458596931374969601L;
  private static final long DEL_BETWEEN_SPACES_MAM = 458596960533028865L;
  private static final long C1_BETWEEN_SPACES_MAM = 458596960999831809L;
  private static final long BOM_BETWEEN_SPACES_MAM = 458596961471870465L;
  private static final long BOM_INSIDE_WORD_MAM = 458596930470176257L;
  private static final long BOM_AT_START_MAM = 458596930569809921L;
  private static final long U10FFFF_BETWEEN_SPACES_MAM = 458596954957278977L;
  private static final long U1FFFE_BETWEEN_SPACES_MAM = 458596955425925121L;
  private static final long U10FFFF_MUSIC_AND_PRIVATE_USE_MAM = 458596931173025793L;
  private static final long REPLACEMENT_CHARACTERS_MAM = 458596931579685377L;
  private static final long RAW_SOH_MAM = 458596937922376961L;
  private static final long RAW_BACKSPACE_MAM = 458596939403014401L;
  private static final long RAW_VERTICAL_TAB_MAM = 458596940888635137L;
  private static final long RAW_FORM_FEED_MAM = 458596942370272257L;
  private static final long RAW_SHIFT_OUT_MAM = 458596943854190593L;
  private static final long RAW_UNIT_SEPARATOR_MAM = 458596945340634369L;
  private static final long NUL_BYTE_FIRST_MAM = 458596946827492097L;
  private static final long NUL_BYTE_SECOND_MAM = 458596953005763073L;
  private static final long FFFE_AFTER_WORD_MAM = 458596948313675265L;
  private static final long FFFF_AFTER_WORD_MAM = 458596949796168961L;
  private static final long FFFE_BETWEEN_WORDS_MAM = 458596957565455105L;
  private static final long FFFF_BETWEEN_WORDS_MAM = 458596959051030785L;
  private static final long NBSP_AND_IDEOGRAPHIC_SPACE_MAM = 458596927764188673L;
  private static final long ZERO_WIDTH_SPACE_ONLY_MAM = 458596927864710657L;
  private static final long BODY_65000_MAM = 458596989364983809L;
  private static final long BODY_65319_MAM = 458597048901579521L;
  private static final long BODY_65320_MAM = 458597049526444289L;
  private static final long BODY_65321_MAM = 458597050201186817L;
  private static final long BODY_70000_MAM = 458596989991751681L;
  private static final long BODY_128889_COUNTER_PATTERN_MAM = 458596995465480705L;
  private static final long BODY_130855_MAM = 458597050829941505L;
  private static final long BODY_130856_MAM = 458597051466186497L;
  private static final long BODY_130857_MAM = 458597052101637121L;
  private static final long BODY_140000_MAM = 458596990615254017L;
  private static final long BODY_300000_MAM = 458596991255221505L;
  private static final long BODY_20000_MAM = 458596994223539201L;
  private static final long EMPTY_BODY_DIRECT_ROOM_MAM = 458595960859649281L;
  private static final long NEIGHBOUR_IN_DIRECT_ROOM_MAM = 458595960606606337L;
  private static final long EMPTY_BODY_GROUP_ROOM_MAM = 458596047264831489L;
  private static final long NEIGHBOUR_IN_GROUP_ROOM_MAM = 458596047663455233L;
  private static final long EMPTY_BODY_EDGE_1_MAM = 458596926728278017L;
  private static final long EMPTY_BODY_EDGE_2_MAM = 458596927039332865L;
  private static final long EMPTY_BODY_EDGE_3_MAM = 458596927353068801L;
  private static final long EMPTY_BODY_EDGE_4_MAM = 458596927665406721L;
  private static final long EMPTY_BODY_EDGE_5_MAM = 458596929971914497L;

  private static ImportRun run;
  private static ImportedChats chats;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  private static String importedText(String roomId, long mamId) {
    return chats
        .message(roomId, mamId)
        .orElseThrow(() -> new AssertionError("message " + mamId + " was not imported"))
        .text();
  }

  private static String sha256(String text) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private record Expected(String room, long mam, String text) {}

  private static void assertImported(List<Expected> expected) {
    assertAll(
        expected.stream()
            .map(
                e ->
                    (Executable)
                        () ->
                            assertEquals(
                                e.text(), importedText(e.room(), e.mam()), "mam " + e.mam())));
  }

  private record Large(
      String room, long mam, int length, String prefix, String suffix, String sha256) {}

  private static void assertLarge(List<Large> expected) {
    assertAll(
        expected.stream()
            .map(
                e ->
                    (Executable)
                        () -> {
                          String text = importedText(e.room(), e.mam());
                          assertEquals(e.length(), text.length(), "length of mam " + e.mam());
                          assertEquals(
                              e.prefix(),
                              text.substring(0, e.prefix().length()),
                              "prefix of mam " + e.mam());
                          assertEquals(
                              e.suffix(),
                              text.substring(text.length() - e.suffix().length()),
                              "suffix of mam " + e.mam());
                          assertEquals(e.sha256(), sha256(text), "sha256 of mam " + e.mam());
                        }));
  }

  @Nested
  @DisplayName("TEXT-1: text comes from the first direct-child body, never from search_body")
  class FirstDirectBody {

    @Test
    @DisplayName("TEXT-1: text keeps case, punctuation and symbols that search_body strips")
    void textKeepsPunctuationAndCaseThatSearchBodyLoses() {
      assertImported(
          List.of(
              new Expected(
                  GROUP_C_ROOM, HYPHEN_COMMA_PUNCTUATION_MAM, "L5 oremi psum, dolor-sitamet"),
              new Expected(
                  DIRECT_B_ROOM, MIXED_CASE_SHARP_S_MAM, "Lo\u00fc\u00dfr emi P\u00fcsumdo"),
              new Expected(
                  GROUP_A_ROOM, WINDOWS_PATH_BACKSLASHES_MAM, "L:\\Oremi\\psumdo\\Lorsitame"),
              new Expected(
                  DIRECT_B_ROOM, URL_WITH_SCHEME_MAM, "lor://emips.umdolor.sit/ame/tcon.sec.te"),
              new Expected(
                  DIRECT_B_ROOM,
                  CODE_FENCE_WITH_QUOTES_MAM,
                  "```lore\n"
                      + "mipsum dolors itam etco(Nsecte[] t) {\n"
                      + "    Uradip.isc.ingelit(\"lo & lor\");\n"
                      + "}\n"
                      + "```"),
              new Expected(GROUP_A_ROOM, CDATA_END_MARKER_MAM, "]]> lorem ips")));
    }

    @Test
    @DisplayName("TEXT-1: emoji-only bodies keep the emoji (search_body is empty)")
    void emojiOnlyBodiesAreNotEmptiedLikeSearchBody() {
      assertImported(
          List.of(
              new Expected(DIRECT_A_ROOM, EMOJI_ONLY_GRIN_MAM, "\ud83d\ude00"),
              new Expected(DIRECT_B_ROOM, EMOJI_ONLY_THUMBS_UP_MAM, "\ud83d\udc4d"),
              new Expected(DIRECT_B_ROOM, EMOJI_ONLY_THINKING_MAM, "\ud83e\udd14")));
    }

    @Test
    @DisplayName("TEXT-1: leading and trailing spaces of non-blank text are never trimmed")
    void leadingAndTrailingSpacesAreKept() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, LEADING_AND_TRAILING_SPACES_MAM, "  loremi psum  "),
              new Expected(GROUP_A_ROOM, TRAILING_SPACES_ONLY_MAM, "loremips umdolo   "),
              new Expected(EDGE_ROOM, TRAILING_SPACE_AFTER_MARKUP_MAM, "<l>lore</l> & lor ")));
    }

    @Test
    @DisplayName("TEXT-1/C36: with several direct bodies the first one is the text")
    void firstOfSeveralDirectBodiesWins() {
      assertImported(List.of(new Expected(EDGE_ROOM, THREE_BODIES_MAM, "lorem")));
    }

    @Test
    @DisplayName("TEXT-1: xml:lang and foreign attributes on body are ignored")
    void bodyAttributesDoNotChangeText() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, BODY_LANG_IT_MAM, "lore mipsumdo"),
              new Expected(EDGE_ROOM, BODY_LANG_EMPTY_MAM, "lore mipsu"),
              new Expected(EDGE_ROOM, BODY_FOREIGN_ATTRIBUTE_MAM, "lore mipsu")));
    }

    @Test
    @DisplayName("TEXT-1/C32: a body nested in an html element is never read")
    void nestedXhtmlBodyIsNotTheText() {
      assertImported(List.of(new Expected(EDGE_ROOM, BODY_THEN_XHTML_BODY_MAM, "lorem")));
    }

    @Test
    @DisplayName("TEXT-1/C40: the deep text of the direct body is taken, child tags dropped")
    void childElementsInsideBodyContributeTheirText() {
      assertImported(List.of(new Expected(EDGE_ROOM, BODY_WITH_BOLD_CHILD_MAM, "a lore c")));
    }

    @Test
    @DisplayName("TEXT-1: reply rows whose reply element precedes the body keep the body text")
    void replyElementBeforeBodyDoesNotHideText() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, IOS_REPLY_BEFORE_BODY_MAM, "lor emips"),
              new Expected(GROUP_D_ROOM, GROUP_REPLY_BEFORE_BODY_MAM, "L8 ore mipsu")));
    }

    @Test
    @DisplayName("TEXT-7/C42: a 100 KB stanza carrying a four-character body")
    void smallBodyInsideHugeStanzaIsImportedExactly() {
      assertImported(List.of(new Expected(EDGE_ROOM, SMALL_BODY_HUGE_STANZA_MAM, "lore")));
    }
  }

  @Nested
  @DisplayName("TEXT-1/C18: XML entities are decoded exactly once")
  class XmlEntities {

    @Test
    @DisplayName("C18: the five predefined XML entities are decoded exactly once")
    void predefinedEntitiesAreDecodedOnce() {
      assertImported(
          List.of(
              new Expected(
                  GROUP_C_ROOM,
                  EMOJI_AMP_AND_ANGLE_BRACKETS_MAM,
                  "L5 orem ip sumdolo (rsita) \ud83d\ude00 & <l>"),
              new Expected(
                  EDGE_ROOM,
                  QUOTES_AND_BACKTICKS_MAM,
                  "lo'l \"lo\" 'lo' `lo` \u2018re\u2019 \u201cmi\u201d"),
              new Expected(DIRECT_B_ROOM, ENTITIES_ONLY_AMP_LT_GT_MAM, "l & l < l > l")));
    }

    @Test
    @DisplayName("C18: stored &amp;x; becomes the literal text &x;, not a second decode")
    void doubleEscapedEntitiesStayOnceUnescaped() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, AMP_THEN_QUOTES_ENTITY_MAM, "'l' \"l\" &lo;r&lo; &lor;"),
              new Expected(EDGE_ROOM, AMP_L_SEMICOLON_MAM, "&l;"),
              new Expected(DIRECT_B_ROOM, AMP_NUMERIC_REFERENCE_MAM, "&#128512; loremip sumdol"),
              new Expected(EDGE_ROOM, AMP_NAMED_REFERENCE_MAM, "loremi &loremi; psumd")));
    }

    @Test
    @DisplayName("C18: &lt;tag&gt; typed by the user is plain text")
    void escapedMarkupBecomesLiteralText() {
      assertImported(
          List.of(
              new Expected(GROUP_A_ROOM, ESCAPED_TAG_PAIR_MAM, "<loremi>lorem(1)</loremi>"),
              new Expected(GROUP_A_ROOM, ESCAPED_COMPARISON_MAM, "5 > 3 && 2 < 4"),
              new Expected(
                  EDGE_ROOM,
                  ESCAPED_TAGS_AND_BRACES_MAM,
                  "{\"l\":1} <loremi>lorem(1)</loremi> \\l \\o"),
              new Expected(EDGE_ROOM, ESCAPED_TAG_INLINE_MAM, "lor <l> lor & lore")));
    }
  }

  @Nested
  @DisplayName("TEXT-1: Unicode content is kept as stored")
  class UnicodeContent {

    @Test
    @DisplayName("TEXT-1: non-BMP emoji, ZWJ sequences, flags and skin tones are kept as stored")
    void nonBmpEmojiSurviveIntact() {
      assertImported(
          List.of(
              new Expected(
                  DIRECT_B_ROOM,
                  ACCENT_AND_THREE_FACES_MAM,
                  "\u00dcloremipsu \ud83d\ude02\ud83d\ude02\ud83d\ude02"),
              new Expected(
                  GROUP_A_ROOM, PRAYER_HANDS_AFTER_LETTERS_MAM, "l\u00e9o\u00e0 re \ud83d\ude4f"),
              new Expected(
                  EDGE_ROOM,
                  FAMILY_THUMBS_FLAG_KEYCAP_MAM,
                  "\ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67 \ud83d\udc4d\ud83c\udffd"
                      + " \ud83c\uddee\ud83c\uddf9 1\ufe0f\u20e3"),
              new Expected(
                  GROUP_A_ROOM, LETTER_AND_FLAG_MAM, "\u00dcloremipsu \ud83c\uddee\ud83c\uddf9"),
              new Expected(
                  GROUP_A_ROOM,
                  TECHNOLOGIST_ZWJ_SEQUENCE_MAM,
                  "Lo\u00fc\u00dfr emi P\u00fcsumdo \ud83e\uddd1\ud83c\udffb\u200d\ud83d\udcbb"),
              new Expected(
                  GROUP_A_ROOM, RED_HEART_VARIATION_SELECTOR_MAM, "loremip su 5 \u2764\ufe0f")));
    }

    @Test
    @DisplayName("TEXT-1: Hebrew, Cyrillic, Japanese, Arabic and Thai are kept as stored")
    void nonLatinScriptsSurviveIntact() {
      assertImported(
          List.of(
              new Expected(
                  GROUP_A_ROOM,
                  HEBREW_MAM,
                  "\u05e9\u05dc\u05d5\u05dd \u05dc\u05db\u05d5\u05dc\u05dd"),
              new Expected(
                  GROUP_A_ROOM,
                  CYRILLIC_QUESTION_MAM,
                  "\u041a\u043e\u0433\u0434\u0430 \u0432\u0441\u0442\u0440\u0435\u0447\u0430?"),
              new Expected(
                  GROUP_A_ROOM,
                  CYRILLIC_SENTENCE_MAM,
                  "\u0425\u043e\u0440\u043e\u0448\u043e,"
                      + " \u0434\u043e\u0433\u043e\u0432\u043e\u0440\u0438\u043b\u0438\u0441\u044c"),
              new Expected(GROUP_A_ROOM, JAPANESE_MAM, "\u4e86\u89e3\u3057\u307e\u3057\u305f"),
              new Expected(
                  EDGE_ROOM,
                  MIXED_CJK_ARABIC_HEBREW_THAI_MAM,
                  "\u4f60\u597d \u0645\u0631\u062d\u0628\u0627 \u05e9\u05dc\u05d5\u05dd"
                      + " \u0e2a\u0e27\u0e31\u0e2a\u0e14\u0e35")));
    }

    @Test
    @DisplayName(
        "TEXT-1: combining marks, bidi controls and zero-width joiners are not normalized away")
    void combiningAndFormatCharactersSurviveIntact() {
      assertImported(
          List.of(
              new Expected(
                  EDGE_ROOM, COMBINING_MARKS_MAM, "l\u0301 o\u0351\u0356r\u0489e\u0360m\u0361i"),
              new Expected(EDGE_ROOM, BIDI_OVERRIDES_MAM, "lor\u202eemi\u202c \u2067p\u2069"),
              new Expected(EDGE_ROOM, ZERO_WIDTH_CHARACTERS_MAM, "l\u200bo\u200cr\u200de\u2060m")));
    }
  }

  @Nested
  @DisplayName("TEXT-1/C29: multiline bodies and end-of-line handling")
  class Multiline {

    @Test
    @DisplayName("TEXT-1: multiline bodies keep leading, trailing and repeated newlines")
    void lineFeedBodiesKeepEveryNewline() {
      assertImported(
          List.of(
              new Expected(DIRECT_A_ROOM, QUOTED_THREE_LINES_MAM, "> lorem\n> loremi\npsumd olors"),
              new Expected(
                  GROUP_A_ROOM, BLANK_LINES_BETWEEN_MAM, "lore1\n\n\nmips4 umdol 2 orsit ametc"),
              new Expected(GROUP_A_ROOM, LEADING_NEWLINE_MAM, "\nloremi psum dolorsi"),
              new Expected(DIRECT_B_ROOM, TRAILING_NEWLINE_MAM, "lore\n"),
              new Expected(GROUP_A_ROOM, CODE_FENCE_TWO_LINES_MAM, "``` loremips umdol\norsi"),
              new Expected(GROUP_A_ROOM, LIST_DASHES_AND_INDENT_MAM, "- l\n- o\n  - r\n- e")));
    }

    @Test
    @DisplayName("TEXT-1/C29: stored CR and CRLF come out as LF, as the XML parser reports them")
    void carriageReturnsAreNormalizedToLineFeed() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, CRLF_AND_LONE_CR_MAM, "lore1\nmips2\numdo3\nlors4"),
              new Expected(EDGE_ROOM, SINGLE_CRLF_MAM, "l\no\nr")));
    }

    @Test
    @DisplayName("TEXT-1: TAB, U+2028, U+2029 and U+0085 are kept as stored")
    void tabAndUnicodeLineSeparatorsAreKept() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, TAB_LF_CR_MAM, "l\to\nr\ne"),
              new Expected(EDGE_ROOM, UNICODE_LINE_SEPARATORS_MAM, "l\u2028o\u2029r\u0085e")));
    }
  }

  @Nested
  @DisplayName("TEXT-3/O17: typed backslash sequences are not decoded in plain text")
  class LiteralBackslashes {

    @Test
    @DisplayName("O17: a typed backslash-uXXXX in plain text is not decoded")
    void typedUnicodeEscapesInPlainTextStayVerbatim() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, TYPED_U00E8_AND_U0041_MAM, "lore\\u00e8 \\u0041 loremip"),
              new Expected(
                  EDGE_ROOM, TYPED_SURROGATE_PAIR_ESCAPES_MAM, "lorem \\ud83d\\ude00 loremip"),
              new Expected(
                  DIRECT_B_ROOM,
                  TYPED_U00E9_IN_DIRECT_ROOM_MAM,
                  "loremip \\u00e9 loremi psumd ol orsi"),
              new Expected(
                  DIRECT_B_ROOM,
                  TYPED_U00E9_SECOND_ROW_MAM,
                  "loremip \\u00e9 loremi psumd ol orsi"),
              new Expected(
                  GROUP_E_ROOM,
                  TYPED_U00E8_IN_OTHER_GROUP_MAM,
                  "lore\\u00e8 lorem ipsumdolo \\u0041")));
    }

    @Test
    @DisplayName("O17: backslashes that are not unicode escapes are kept")
    void otherBackslashSequencesStayVerbatim() {
      assertImported(
          List.of(
              new Expected(
                  EDGE_ROOM,
                  BACKSLASH_E_S_AND_DOUBLE_BACKSLASH_U_MAM,
                  "lor \\e mip \\s12 umd \\\\u0041"),
              new Expected(GROUP_A_ROOM, LEADING_BACKSLASH_L_MAM, "\\l or emi p sumdolo")));
    }
  }

  @Nested
  @DisplayName("TEXT-5/V2: legal but unusual characters are not filtered")
  class LegalUnusualCharacters {

    @Test
    @DisplayName(
        "TEXT-5: DEL, C1, BOM, noncharacters beyond the BMP and private use are legal XML and kept")
    void legalButUnusualCharactersAreKept() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, DEL_AND_C1_CONTROLS_MAM, "l\u007fo\u0080r\u009fe"),
              new Expected(EDGE_ROOM, DEL_BETWEEN_SPACES_MAM, "lor \u007f emip"),
              new Expected(EDGE_ROOM, C1_BETWEEN_SPACES_MAM, "lor \u0080 emip"),
              new Expected(EDGE_ROOM, BOM_BETWEEN_SPACES_MAM, "lor \ufeff emip"),
              new Expected(EDGE_ROOM, BOM_INSIDE_WORD_MAM, "l\ufeffo"),
              new Expected(EDGE_ROOM, BOM_AT_START_MAM, "\ufefflorem ips"),
              new Expected(EDGE_ROOM, U10FFFF_BETWEEN_SPACES_MAM, "loremi \udbff\udfff psumd"),
              new Expected(EDGE_ROOM, U1FFFE_BETWEEN_SPACES_MAM, "loremi \ud83f\udffe psumd"),
              new Expected(
                  EDGE_ROOM, U10FFFF_MUSIC_AND_PRIVATE_USE_MAM, "\udbff\udfff \ud834\udd1e \ue000"),
              new Expected(EDGE_ROOM, REPLACEMENT_CHARACTERS_MAM, "\ufffd \ufffd")));
    }
  }

  @Nested
  @DisplayName("TEXT-5/TEXT-V2: XML-illegal characters are dropped and the row is still imported")
  class IllegalCharacters {

    @Test
    @DisplayName(
        "TEXT-5/TEXT-V2/C19: raw C0 characters stored by MongooseIM are dropped and the row is"
            + " imported")
    void rawC0ControlCharactersAreDropped() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, RAW_SOH_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, RAW_BACKSPACE_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, RAW_VERTICAL_TAB_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, RAW_FORM_FEED_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, RAW_SHIFT_OUT_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, RAW_UNIT_SEPARATOR_MAM, "loremi  psumd")));
    }

    @Test
    @DisplayName("TEXT-5/TEXT-V2/C20: the two rows with a NUL byte are imported without it")
    void nulBytesAreDroppedAndRowIsImported() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, NUL_BYTE_FIRST_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, NUL_BYTE_SECOND_MAM, "loremi  psumd")));
    }

    @Test
    @DisplayName("TEXT-5/TEXT-V2: U+FFFE and U+FFFF are dropped and the row is imported")
    void noncharactersFffeAndFfffAreDropped() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, FFFE_AFTER_WORD_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, FFFF_AFTER_WORD_MAM, "loremi  psumd"),
              new Expected(EDGE_ROOM, FFFE_BETWEEN_WORDS_MAM, "lor  emip"),
              new Expected(EDGE_ROOM, FFFF_BETWEEN_WORDS_MAM, "lor  emip")));
    }
  }

  @Nested
  @DisplayName("TEXT-8 / C4: empty plain messages are imported with empty text")
  class EmptyBodies {

    private final List<Expected> emptyRows =
        List.of(
            new Expected(DIRECT_A_ROOM, EMPTY_BODY_DIRECT_ROOM_MAM, ""),
            new Expected(GROUP_A_ROOM, EMPTY_BODY_GROUP_ROOM_MAM, ""),
            new Expected(EDGE_ROOM, EMPTY_BODY_EDGE_1_MAM, ""),
            new Expected(EDGE_ROOM, EMPTY_BODY_EDGE_2_MAM, ""),
            new Expected(EDGE_ROOM, EMPTY_BODY_EDGE_3_MAM, ""),
            new Expected(EDGE_ROOM, EMPTY_BODY_EDGE_4_MAM, ""),
            new Expected(EDGE_ROOM, EMPTY_BODY_EDGE_5_MAM, ""));

    @Test
    @DisplayName("TEXT-8 / C4: plain rows with an empty body are imported as messages")
    void emptyBodyPlainRowsAreImported() {
      assertAll(
          () -> assertTrue(chats.message(DIRECT_A_ROOM, NEIGHBOUR_IN_DIRECT_ROOM_MAM).isPresent()),
          () -> assertTrue(chats.message(GROUP_A_ROOM, NEIGHBOUR_IN_GROUP_ROOM_MAM).isPresent()),
          () -> assertTrue(chats.message(EDGE_ROOM, NBSP_AND_IDEOGRAPHIC_SPACE_MAM).isPresent()));
      assertImported(emptyRows);
    }

    @Test
    @DisplayName("TEXT-8 / C4: an empty plain row is a message, never a system event")
    void emptyBodyRowsAreMessagesNotSystemEvents() {
      List<Executable> checks = new ArrayList<>();
      for (Expected row : emptyRows) {
        checks.add(
            () ->
                assertFalse(
                    chats.systemEvent(row.room(), row.mam()).isPresent(), "mam " + row.mam()));
      }
      assertAll(checks);
    }

    @Test
    @DisplayName(
        "TEXT-8: text that is blank only for Java (NBSP, ideographic space, ZWSP) is not trimmed"
            + " and is imported")
    void nonCollapsedBlankTextIsImportedUntrimmed() {
      assertImported(
          List.of(
              new Expected(EDGE_ROOM, NBSP_AND_IDEOGRAPHIC_SPACE_MAM, "\u00a0\u3000"),
              new Expected(EDGE_ROOM, ZERO_WIDTH_SPACE_ONLY_MAM, "\u200b")));
    }
  }

  @Nested
  @DisplayName("TEXT-7/C42: very large bodies are imported verbatim")
  class LargeBodies {

    @Test
    @DisplayName("TEXT-7/C42: bodies of 65,000 to 65,321 chars are imported verbatim")
    void bodiesAroundSixtyFiveThousandCharsAreImportedVerbatim() {
      assertLarge(
          List.of(
              new Large(
                  EDGE_ROOM,
                  BODY_65000_MAM,
                  65000,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "uipexeacommodoloremipsumdolorsitametcons",
                  "53e2859241729c6ec8e0f3e46ea4a8dd9364be5c8e0cc2db574dd142633dae56"),
              new Large(
                  EDGE_ROOM,
                  BODY_65319_MAM,
                  65319,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "isnostrudexercitationullamcolaborisnisiu",
                  "81a90d3bbcde9b8a29f6a25eaf58aafb26cc73f5830de3e006491d75d637896a"),
              new Large(
                  EDGE_ROOM,
                  BODY_65320_MAM,
                  65320,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "snostrudexercitationullamcolaborisnisiut",
                  "559022142008dd392135bac0cb550432c8241604ecafe224276d7c85832e8c99"),
              new Large(
                  EDGE_ROOM,
                  BODY_65321_MAM,
                  65321,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "nostrudexercitationullamcolaborisnisiuta",
                  "5449cf9d68ec20a765ec6775ebd919d1dfb85172624a2c8a711d3b12bd01722f")));
    }

    @Test
    @DisplayName("TEXT-7/C42: bodies of 128,889 to 130,857 chars are imported verbatim")
    void bodiesAroundOneHundredTwentyEightKiBAreImportedVerbatim() {
      assertLarge(
          List.of(
              new Large(
                  EDGE_ROOM,
                  BODY_128889_COUNTER_PATTERN_MAM,
                  128889,
                  "l0 o1 r2 e3 m4 i5 p6 s7 u8 m9 d10 o11 l1",
                  "19994 s19995 n19996 i19997 s19998 i19999",
                  "2355dbb31c9b9d86cdfd924703effd7225ab5362960053699e1f037bc4eeb9b4"),
              new Large(
                  EDGE_ROOM,
                  BODY_130855_MAM,
                  130855,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "ationullamcolaborisnisiutaliquipexeacomm",
                  "b172461f17895399c9148eaf0cbef77b75c677b0d87e131f8cc765450a6fdc36"),
              new Large(
                  EDGE_ROOM,
                  BODY_130856_MAM,
                  130856,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "tionullamcolaborisnisiutaliquipexeacommo",
                  "dc05211c79d9ba6ea4a7afa37083e7d4c9399fde324d10e1321da7873bafe713"),
              new Large(
                  EDGE_ROOM,
                  BODY_130857_MAM,
                  130857,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "ionullamcolaborisnisiutaliquipexeacommod",
                  "cec62f3cafda9271ffb87a9d14c0809ee652d161db56b88c215c999ad6c564aa")));
    }

    @Test
    @DisplayName(
        "TEXT-7/C42: bodies of 20,000, 70,000, 140,000 and 300,000 chars are imported verbatim")
    void bodiesUpToThreeHundredThousandCharsAreImportedVerbatim() {
      assertLarge(
          List.of(
              new Large(
                  EDGE_ROOM,
                  BODY_20000_MAM,
                  20000,
                  "\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9",
                  "\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9",
                  "0d2b714f0bbfd34d4c5672cd0220630d3796f36b5a962798b81aa6ca8bf9b040"),
              new Large(
                  EDGE_ROOM,
                  BODY_70000_MAM,
                  70000,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "untutlaboreetdoloremagnaaliquautenimadmi",
                  "ff87803314f2301e66af869027020f70ca66cb252160861834b59cbd002f9420"),
              new Large(
                  EDGE_ROOM,
                  BODY_140000_MAM,
                  140000,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "remipsumdolorsitametconsecteturadipiscin",
                  "ad4b36fb6a6560fe74397ae884bd65af212f7769b0b81bdac97f186cb6191661"),
              new Large(
                  EDGE_ROOM,
                  BODY_300000_MAM,
                  300000,
                  "loremipsumdolorsitametconsecteturadipisc",
                  "nsecteturadipiscingelitseddoeiusmodtempo",
                  "31428c4a059aec5ba503106780033f2a9a5fd6987a019095d77b230c7565d768")));
    }
  }

  @Nested
  @DisplayName("TEXT-9: no message text reaches the migration output")
  class OutputHygiene {

    @Test
    @DisplayName(
        "TEXT-9: neither decoded texts nor raw stored XML of imported rows appear in logs or"
            + " failure output")
    void messageContentIsNeverLogged() {
      List<Expected> pinned =
          List.of(
              new Expected(
                  GROUP_C_ROOM, HYPHEN_COMMA_PUNCTUATION_MAM, "L5 oremi psum, dolor-sitamet"),
              new Expected(
                  DIRECT_B_ROOM,
                  CODE_FENCE_WITH_QUOTES_MAM,
                  "```lore\n"
                      + "mipsum dolors itam etco(Nsecte[] t) {\n"
                      + "    Uradip.isc.ingelit(\"lo & lor\");\n"
                      + "}\n"
                      + "```"),
              new Expected(
                  GROUP_C_ROOM,
                  EMOJI_AMP_AND_ANGLE_BRACKETS_MAM,
                  "L5 orem ip sumdolo (rsita) \ud83d\ude00 & <l>"),
              new Expected(EDGE_ROOM, TYPED_U00E8_AND_U0041_MAM, "lore\\u00e8 \\u0041 loremip"),
              new Expected(
                  GROUP_A_ROOM, BLANK_LINES_BETWEEN_MAM, "lore1\n\n\nmips4 umdol 2 orsit ametc"),
              new Expected(EDGE_ROOM, RAW_SOH_MAM, "loremi  psumd"),
              new Expected(
                  EDGE_ROOM,
                  MIXED_CJK_ARABIC_HEBREW_THAI_MAM,
                  "\u4f60\u597d \u0645\u0631\u062d\u0628\u0627 \u05e9\u05dc\u05d5\u05dd"
                      + " \u0e2a\u0e27\u0e31\u0e2a\u0e14\u0e35"),
              new Expected(
                  EDGE_ROOM,
                  FAMILY_THUMBS_FLAG_KEYCAP_MAM,
                  "\ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67 \ud83d\udc4d\ud83c\udffd"
                      + " \ud83c\uddee\ud83c\uddf9 1\ufe0f\u20e3"),
              new Expected(EDGE_ROOM, AMP_THEN_QUOTES_ENTITY_MAM, "'l' \"l\" &lo;r&lo; &lor;"));
      String output = run.allOutput();
      assertAll(
          () -> assertImported(pinned),
          () ->
              assertAll(
                  pinned.stream()
                      .map(
                          e ->
                              (Executable)
                                  () ->
                                      assertFalse(
                                          output.contains(e.text()),
                                          "text of mam " + e.mam() + " leaked"))),
          () -> assertFalse(output.contains("<body")),
          () -> assertFalse(output.contains("<message ")));
    }
  }
}
