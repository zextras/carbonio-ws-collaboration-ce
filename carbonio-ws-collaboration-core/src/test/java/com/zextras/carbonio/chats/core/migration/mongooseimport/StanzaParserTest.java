// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow.Kind;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class StanzaParserTest {

  private static final String FROM =
      "from='6457d489-8eeb-46f9-abf4-d14fb63ddc65@muclight.carbonio/"
          + "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8@carbonio' type='groupchat'";
  private static final String MUC_USER =
      "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member'"
          + " jid='bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8@carbonio/1791-391246-309803-72711060583c110e'"
          + " role='participant'/></x>";

  private static String stanza(String id, String content) {
    return "<message " + FROM + " id='" + id + "'>" + content + MUC_USER + "</message>";
  }

  private static ParsedRow parse(String xml) {
    return StanzaParser.parse(xml.getBytes(StandardCharsets.UTF_8));
  }

  private static ParsedRow inStanza(String content) {
    return parse(stanza("cf1ada7c-1890-44b3-9c88-10b2f89e2663", content));
  }

  private static final int MAX_ROW_BYTES = 33_554_432;
  private static final long ALLOCATION_BUDGET = 128L * 1024 * 1024;

  private static byte[] largestRow(String head, String unit, String tail) {
    StringBuilder xml = new StringBuilder(MAX_ROW_BYTES).append(head);
    while (xml.length() + unit.length() + tail.length() <= MAX_ROW_BYTES) {
      xml.append(unit);
    }
    return xml.append(tail).toString().getBytes(StandardCharsets.UTF_8);
  }

  private static long allocatedBy(Runnable work) {
    com.sun.management.ThreadMXBean threads =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    long before = threads.getCurrentThreadAllocatedBytes();
    work.run();
    return threads.getCurrentThreadAllocatedBytes() - before;
  }

  @Test
  @DisplayName("Plain text row keeps the root client id")
  void plainText() {
    ParsedRow row = inStanza("<body>L4 oremi ps</body><markable xmlns='urn:xmpp:chat-markers:0'/>");
    assertEquals(Kind.TEXT, row.kind());
    assertEquals("L4 oremi ps", row.text());
    assertEquals("cf1ada7c-1890-44b3-9c88-10b2f89e2663", row.rootId());
    assertNull(row.replyRef());
  }

  @Test
  @DisplayName("Reply with a stray brace in @to, reply after the body")
  void replyWithStrayBrace() {
    ParsedRow row =
        inStanza(
            "<body>L4 oremi ps 1:1</body><markable xmlns='urn:xmpp:chat-markers:0'/><reply"
                + " id='CNA2ODTV41G1'"
                + " to='a793945e-d600-5362-8ecf-bb6b1d88dbc3@carbonio/99b17723-465e-4f87-bb1c-429687075b0d@muclight.carbonio}'"
                + " xmlns='urn:xmpp:reply:0'/>");
    assertEquals(Kind.TEXT, row.kind());
    assertEquals("CNA2ODTV41G1", row.replyRef());
  }

  @Test
  @DisplayName("Reply before the body does not hide it")
  void replyBeforeBody() {
    ParsedRow row =
        inStanza("<reply id='CNA2P086KAO1' xmlns='urn:xmpp:reply:0'/><body>L8 ore mipsu</body>");
    assertEquals(Kind.TEXT, row.kind());
    assertEquals("L8 ore mipsu", row.text());
    assertEquals("CNA2P086KAO1", row.replyRef());
  }

  @Test
  @DisplayName("Only the first reply in the reply namespace counts")
  void replyNamespaceAndOrder() {
    ParsedRow row =
        inStanza(
            "<body>x</body><reply id='A' xmlns='urn:xmpp:reply:1'/><reply id='B'"
                + " xmlns='urn:xmpp:reply:0'/><reply id='C' xmlns='urn:xmpp:reply:0'/>");
    assertEquals("B", row.replyRef());
    assertNull(inStanza("<body>x</body><reply xmlns='urn:xmpp:reply:0'/>").replyRef());
  }

  @Test
  @DisplayName("Three bodies: the first wins")
  void threeBodies() {
    assertEquals("one", inStanza("<body>one</body><body>two</body><body>three</body>").text());
  }

  @Test
  @DisplayName("xml:lang body is the body, xhtml body nested in html is ignored")
  void xhtmlBody() {
    ParsedRow row =
        inStanza(
            "<html xmlns='http://jabber.org/protocol/xhtml-im'><body"
                + " xmlns='http://www.w3.org/1999/xhtml'><p>rich <b>lore</b></p></body></html>"
                + "<body xml:lang='en'>lorem</body>");
    assertEquals("lorem", row.text());
    assertEquals(
        Kind.SKIP,
        inStanza(
                "<html xmlns='http://jabber.org/protocol/xhtml-im'><body"
                    + " xmlns='http://www.w3.org/1999/xhtml'><p>rich</p></body></html>")
            .kind());
  }

  @Test
  @DisplayName("Child tags inside the body contribute only their text")
  void deepText() {
    assertEquals("a lore c", inStanza("<body>a <b>lore</b> c</body>").text());
  }

  @Test
  @DisplayName("Entities are decoded once")
  void entities() {
    assertEquals(
        "&lt; &#128512; <l> & \"q\"",
        inStanza("<body>&amp;lt; &amp;#128512; &lt;l&gt; &amp; &quot;q&quot;</body>").text());
    assertEquals("\uD83D\uDE00", inStanza("<body>&#128512;</body>").text());
  }

  @Test
  @DisplayName("CR and CRLF become LF, spaces and newlines are not trimmed")
  void lineEnds() {
    assertEquals(
        "lore1\nmips2\numdo3\nlors4", inStanza("<body>lore1\nmips2\r\numdo3\rlors4</body>").text());
    assertEquals("\n  x \n", inStanza("<body>\n  x \n</body>").text());
  }

  @Test
  @DisplayName("Tab and unicode separators are kept")
  void tabAndSeparators() {
    assertEquals(
        "a\tb\u2028c\u2029d\u0085e", inStanza("<body>a\tb\u2028c\u2029d\u0085e</body>").text());
  }

  @Test
  @DisplayName("Raw C0 and NUL bytes are dropped and the row is imported")
  void c0AndNulBytes() {
    for (int c : new int[] {0x00, 0x01, 0x08, 0x0b, 0x0c, 0x0e, 0x1f}) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] head =
          ("<message " + FROM + " id='83a94146'><body>loremi ").getBytes(StandardCharsets.UTF_8);
      out.writeBytes(head);
      out.write(c);
      out.writeBytes((" psumd</body>" + MUC_USER + "</message>").getBytes(StandardCharsets.UTF_8));
      ParsedRow row = StanzaParser.parse(out.toByteArray());
      assertEquals(Kind.TEXT, row.kind(), "char " + c);
      assertEquals("loremi  psumd", row.text(), "char " + c);
    }
  }

  @Test
  @DisplayName("U+FFFE and U+FFFF are dropped, other unusual characters are kept")
  void noncharacters() {
    assertEquals("lor  emip", inStanza("<body>lor \uFFFE emip</body>").text());
    assertEquals("lor  emip", inStanza("<body>lor \uFFFF emip</body>").text());
    String legal = "\u007f\u0085\uFEFF\uD83F\uDFFE\uDBFF\uDFFF\uE000\uFFFD\u202e\u200d";
    assertEquals(legal, inStanza("<body>" + legal + "</body>").text());
  }

  @Test
  @DisplayName("Verbatim backslash sequences in plain text")
  void literalBackslashes() {
    assertEquals(
        "\\e \\s \\\\u0041 \\u00e8", inStanza("<body>\\e \\s \\\\u0041 \\u00e8</body>").text());
  }

  @Test
  @DisplayName("Empty and self-closing bodies are text with empty text, no body is skipped")
  void emptyBodies() {
    for (String body : new String[] {"<body/>", "<body></body>"}) {
      ParsedRow row = inStanza(body);
      assertEquals(Kind.TEXT, row.kind());
      assertEquals("", row.text());
    }
    assertEquals(Kind.SKIP, inStanza("<markable xmlns='urn:xmpp:chat-markers:0'/>").kind());
  }

  @Test
  @DisplayName("Blank but non-empty bodies are text")
  void blankBodies() {
    assertEquals(Kind.TEXT, inStanza("<body>\u00a0\u3000</body>").kind());
    assertEquals("\u200b", inStanza("<body>\u200b</body>").text());
  }

  @Test
  @DisplayName("Unparseable input never throws")
  void unparseable() {
    assertEquals(Kind.SKIP, parse("<message><body>x").kind());
    assertEquals(Kind.SKIP, parse("not xml").kind());
    assertEquals(Kind.SKIP, StanzaParser.parse(new byte[0]).kind());
    assertEquals(Kind.SKIP, StanzaParser.parse(null).kind());
    assertEquals(Kind.SKIP, parse("<iq><body>x</body></iq>").kind());
  }

  @Test
  @DisplayName("DTD and external entities are not resolved")
  void hardened() {
    ParsedRow row =
        parse(
            "<!DOCTYPE message [<!ENTITY x SYSTEM"
                + " 'file:///etc/passwd'>]><message><body>a&x;b</body></message>");
    assertEquals(Kind.SKIP, row.kind());
  }

  @Test
  @DisplayName("ESL tombstone carries its retracted stamp")
  void eslTombstone() {
    ParsedRow row =
        inStanza(
            "<retracted stamp='2026-10-07T16:41:01Z'"
                + " xmlns='urn:esl:message-retract-by-stanza-id:0'><stanza-id"
                + " by='99b17723-465e-4f87-bb1c-429687075b0d@muclight.carbonio' id='CNA2OUU64C01'"
                + " xmlns='urn:xmpp:sid:0'/></retracted>");
    assertEquals(Kind.TOMBSTONE, row.kind());
    assertEquals(Instant.parse("2026-10-07T16:41:01Z"), row.retractedStamp());
    assertEquals("", row.text());
  }

  @Test
  @DisplayName("XEP-0424 tombstone carries its retracted stamp")
  void xep0424Tombstone() {
    ParsedRow row =
        inStanza(
            "<retracted stamp='2026-10-07T16:41:01Z' xmlns='urn:xmpp:message-retract:0'>"
                + "<origin-id id='025202be-b9bb-489e-9d94-190613c132e4'/></retracted>");
    assertEquals(Kind.TOMBSTONE, row.kind());
    assertEquals(Instant.parse("2026-10-07T16:41:01Z"), row.retractedStamp());
  }

  @Test
  @DisplayName("Tombstone with an uppercase iOS root id")
  void iosTombstone() {
    ParsedRow row =
        parse(
            stanza(
                "E46F8EB2-0838-4330-94C9-95BA825F4F2D",
                "<retracted stamp='2026-10-07T16:41:01Z'"
                    + " xmlns='urn:esl:message-retract-by-stanza-id:0'><stanza-id id='CNA2OUU64C01'"
                    + " xmlns='urn:xmpp:sid:0'/></retracted>"));
    assertEquals(Kind.TOMBSTONE, row.kind());
    assertEquals("E46F8EB2-0838-4330-94C9-95BA825F4F2D", row.rootId());
  }

  @Test
  @DisplayName("Web edit with parent-id")
  void webEdit() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2O3KS6AO1' parent-id='CNA2O3KS6AO1' xmlns='urn:xmpp:fasten:0'>"
                + "<edit xmlns='zextras:xmpp:edit:0'/><external name='body'/></apply-to>"
                + "<body>L6 oremipsum dolors it ametconse</body>");
    assertEquals(Kind.EDIT, row.kind());
    assertEquals("CNA2O3KS6AO1", row.applyToRef());
    assertEquals("L6 oremipsum dolors it ametconse", row.text());
  }

  @Test
  @DisplayName("Android edit without parent-id")
  void androidEdit() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2P28OOU81' xmlns='urn:xmpp:fasten:0'><edit"
                + " xmlns='zextras:xmpp:edit:0'/><external name='body'/></apply-to><body>L8 ore"
                + " mips UMDOLO</body>");
    assertEquals(Kind.EDIT, row.kind());
    assertEquals("CNA2P28OOU81", row.applyToRef());
  }

  @Test
  @DisplayName("Edit body is verbatim, not escape-decoded")
  void editVerbatim() {
    assertEquals(
        "\\u0041",
        inStanza(
                "<apply-to id='A' xmlns='urn:xmpp:fasten:0'><edit"
                    + " xmlns='zextras:xmpp:edit:0'/></apply-to><body>\\u0041</body>")
            .text());
  }

  @Test
  @DisplayName("Edit with an empty body is an edit with empty text, without body it is skipped")
  void emptyEdit() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA3EKB9SB01' parent-id='CNA3EKB9SB01'"
                + " xmlns='urn:xmpp:fasten:0'><edit xmlns='zextras:xmpp:edit:0'/><external"
                + " name='body'/></apply-to><body/>");
    assertEquals(Kind.EDIT, row.kind());
    assertEquals("", row.text());
    assertEquals(
        Kind.SKIP,
        inStanza(
                "<apply-to id='CNA3EKB9SB01' parent-id='CNA3EKB9SB01'"
                    + " xmlns='urn:xmpp:fasten:0'><edit xmlns='zextras:xmpp:edit:0'/></apply-to>")
            .kind());
  }

  @Test
  @DisplayName("Edit combined with a retract is a retract request")
  void editRetractCombo() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2P28OOU81' xmlns='urn:xmpp:fasten:0'><edit"
                + " xmlns='zextras:xmpp:edit:0'/><retract"
                + " xmlns='urn:esl:message-retract-by-stanza-id:0'/></apply-to><body>x</body>");
    assertEquals(Kind.RETRACT_REQUEST, row.kind());
    assertEquals("CNA2P28OOU81", row.applyToRef());
  }

  @Test
  @DisplayName("ESL and XEP-0424 retract requests, also in a second apply-to")
  void retractRequests() {
    ParsedRow esl =
        inStanza(
            "<apply-to id='CNA2P1QD2CG1' xmlns='urn:xmpp:fasten:0'><retract"
                + " xmlns='urn:esl:message-retract-by-stanza-id:0'/></apply-to>");
    ParsedRow xep =
        inStanza(
            "<apply-to id='025202be' xmlns='urn:xmpp:fasten:0'><retract"
                + " xmlns='urn:xmpp:message-retract:0'/></apply-to>");
    ParsedRow two =
        inStanza(
            "<apply-to id='A' xmlns='urn:xmpp:fasten:0'><edit"
                + " xmlns='zextras:xmpp:edit:0'/></apply-to><apply-to id='B'"
                + " xmlns='urn:xmpp:fasten:0'><retract"
                + " xmlns='urn:xmpp:message-retract:0'/></apply-to><body>x</body>");
    assertEquals(Kind.RETRACT_REQUEST, esl.kind());
    assertEquals(Kind.RETRACT_REQUEST, xep.kind());
    assertEquals("025202be", xep.applyToRef());
    assertEquals(Kind.RETRACT_REQUEST, two.kind());
  }

  @Test
  @DisplayName("Apply-to with an unknown child is skipped")
  void applyToUnknownChild() {
    assertEquals(
        Kind.SKIP,
        inStanza(
                "<apply-to id='A' xmlns='urn:xmpp:fasten:0'><star"
                    + " xmlns='urn:example:star'/></apply-to>")
            .kind());
  }

  @Test
  @DisplayName("Web reaction: external body sibling")
  void reactionWeb() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2P3R0TKG1' xmlns='urn:xmpp:fasten:0'><reaction"
                + " xmlns='zextras:xmpp:reaction:0'/><external"
                + " name='body'/></apply-to><body>\uD83D\uDD25</body>");
    assertEquals(Kind.REACTION, row.kind());
    assertEquals("CNA2P3R0TKG1", row.applyToRef());
    assertEquals("\uD83D\uDD25", row.text());
  }

  @Test
  @DisplayName("React Native reaction: external nested in reaction")
  void reactionReactNative() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2P3R0TKG1' xmlns='urn:xmpp:fasten:0'><reaction"
                + " xmlns='zextras:xmpp:reaction:0'><external name='body'/></reaction></apply-to>"
                + "<body>\uD83D\uDD25</body>");
    assertEquals(Kind.REACTION, row.kind());
    assertEquals("\uD83D\uDD25", row.text());
  }

  @Test
  @DisplayName("Android reaction without external, empty body is a removal")
  void reactionAndroidRemoval() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2P010OCO1' xmlns='urn:xmpp:fasten:0'><reaction"
                + " xmlns='zextras:xmpp:reaction:0'/></apply-to><body/>");
    assertEquals(Kind.REACTION, row.kind());
    assertEquals("", row.text());
    assertEquals(
        "\uD83D\uDE0D",
        inStanza(
                "<apply-to id='A' xmlns='urn:xmpp:fasten:0'><reaction"
                    + " xmlns='zextras:xmpp:reaction:0'/></apply-to><body>\uD83D\uDE0D</body>")
            .text());
  }

  @Test
  @DisplayName("iOS reaction: external name=reaction")
  void reactionIos() {
    ParsedRow row =
        inStanza(
            "<apply-to id='CNA2P010OCO1' xmlns='urn:xmpp:fasten:0'><reaction"
                + " xmlns='zextras:xmpp:reaction:0'/><external"
                + " name='reaction'/></apply-to><body>\uD83D\uDE0D</body>");
    assertEquals(Kind.REACTION, row.kind());
    assertEquals("\uD83D\uDE0D", row.text());
    ParsedRow removal =
        inStanza(
            "<apply-to id='CNA2P010OCO1' xmlns='urn:xmpp:fasten:0'><reaction"
                + " xmlns='zextras:xmpp:reaction:0'/><external"
                + " name='reaction'/></apply-to><body/>");
    assertEquals("", removal.text());
  }

  @Test
  @DisplayName("Legacy XEP-0308 replace is skipped")
  void legacyReplace() {
    assertEquals(
        Kind.SKIP,
        inStanza(
                "<body>loremi psumdol orsi</body><replace id='025202be'"
                    + " xmlns='urn:xmpp:message-correct:0'/>")
            .kind());
  }

  @Test
  @DisplayName("Forward takes the decoded inner body, the inner author and the delay stamp")
  void forward() {
    ParsedRow row =
        parse(
            "<message"
                + " from='a992e390-3e44-4536-aed5-a5e13572d046@muclight.carbonio/ba2c72b5-0128-5656-806c-8a163b58450e@carbonio'"
                + " id='bd7608c2cedfa4ea' type='groupchat'><body/><forwarded count='1'"
                + " xmlns='urn:xmpp:forward:0'><delay stamp='2026-10-07T16:38:56.534571Z'"
                + " xmlns='urn:xmpp:delay'/><message"
                + " from='D39BE5DC-cb00-4ad4-9ae3-03f4f0894b99@muclight.carbonio/ba2c72b5-0128-5656-806c-8a163b58450e@carbonio'"
                + " id='82ac0bca' type='groupchat'><body encoded='UTF-8'>L5 orem ip sumdolo (rsita)"
                + " \\ud83d\\ude00 &amp; &lt;l&gt;</body></message></forwarded>"
                + MUC_USER
                + "</message>");
    assertEquals(Kind.FORWARD, row.kind());
    assertEquals("L5 orem ip sumdolo (rsita) \uD83D\uDE00 & <l>", row.text());
    assertEquals("ba2c72b5-0128-5656-806c-8a163b58450e", row.forwardFrom());
    assertEquals(Instant.parse("2026-10-07T16:38:56.534571Z"), row.forwardStamp());
    assertNull(row.attachmentId());
    assertNull(row.replyRef());
  }

  @Test
  @DisplayName("Forward from a plain user jid with resource, outer description ignored")
  void forwardWithDescription() {
    ParsedRow row =
        inStanza(
            "<body>lor em ips</body><forwarded xmlns='urn:xmpp:forward:0'><delay"
                + " stamp='1999-10-07T10:00:00Z' xmlns='urn:xmpp:delay'/><message"
                + " from='97c353af-4374-5ff3-a3eb-3056c0b4dba9@carbonio/x' id='a9d6adb5'"
                + " to='e0df7f4a-7a19-4ab7-9af7-49be3b7874b6@muclight.carbonio' type='groupchat'"
                + " xmlns='jabber:client'><body>lor</body><forwarded"
                + " xmlns='urn:xmpp:forward:0'><delay stamp='2026-10-07T10:00:00Z'"
                + " xmlns='urn:xmpp:delay'/><message from='x@carbonio/y'"
                + " xmlns='jabber:client'><body>loremip</body></message>"
                + "</forwarded></message></forwarded>");
    assertEquals(Kind.FORWARD, row.kind());
    assertEquals("lor", row.text());
    assertEquals("97c353af-4374-5ff3-a3eb-3056c0b4dba9", row.forwardFrom());
    assertEquals(Instant.parse("1999-10-07T10:00:00Z"), row.forwardStamp());
  }

  @Test
  @DisplayName("Forward without a delay has a null stamp")
  void forwardWithoutStamp() {
    ParsedRow row =
        inStanza(
            "<forwarded xmlns='urn:xmpp:forward:0'><message"
                + " from='97c353af-4374-5ff3-a3eb-3056c0b4dba9@carbonio'>"
                + "<body>x</body></message></forwarded>");
    assertEquals(Kind.FORWARD, row.kind());
    assertNull(row.forwardStamp());
  }

  @Test
  @DisplayName("Forward with an empty inner body is kept, without inner body it is skipped")
  void forwardEmptyInner() {
    assertEquals(
        Kind.SKIP,
        inStanza(
                "<body>outer</body><forwarded xmlns='urn:xmpp:forward:0'><delay"
                    + " stamp='2026-10-07T10:00:00Z' xmlns='urn:xmpp:delay'/><message"
                    + " from='97c353af-4374-5ff3-a3eb-3056c0b4dba9@carbonio/x'"
                    + " xmlns='jabber:client'><apply-to id='CNA3EJUJV581'"
                    + " xmlns='urn:xmpp:fasten:0'><retract"
                    + " xmlns='urn:esl:message-retract-by-stanza-id:0'/></apply-to></message></forwarded>")
            .kind());
    ParsedRow empty =
        inStanza("<forwarded xmlns='urn:xmpp:forward:0'><message><body/></message></forwarded>");
    assertEquals(Kind.FORWARD, empty.kind());
    assertEquals("", empty.text());
    assertEquals(
        Kind.SKIP, inStanza("<body>outer</body><forwarded xmlns='urn:xmpp:forward:0'/>").kind());
  }

  @Test
  @DisplayName("Forward with a nested reply is never a reply")
  void forwardNestedReply() {
    ParsedRow row =
        inStanza(
            "<forwarded xmlns='urn:xmpp:forward:0'><message"
                + " from='97c353af-4374-5ff3-a3eb-3056c0b4dba9@carbonio'><body>x</body><reply"
                + " id='R' xmlns='urn:xmpp:reply:0'/></message></forwarded>");
    assertEquals(Kind.FORWARD, row.kind());
    assertNull(row.replyRef());
  }

  private static ParsedRow attachment(String extra, String body) {
    return inStanza(
        "<x xmlns='urn:xmpp:muclight:0#configuration'><operation>attachmentAdded</operation>"
            + "<attachment-id>7bdb68d2-00a0-4f9d-b966-f846a1d5bec8</attachment-id>"
            + "<filename encoded='UTF-8'>lor é emips.umd</filename><mime-type>image/png</mime-type>"
            + "<size>66</size>"
            + extra
            + "</x>"
            + body);
  }

  @Test
  @DisplayName("Multipart caption is fully escaped and decoded, area is copied")
  void attachmentMultipartCaption() {
    ParsedRow row =
        attachment(
            "<area>1x1</area>",
            "<body"
                + " encoded='UTF-8'>\\u006c\\u006f\\u0072\\u0020\\u0065\\u006d\\u0069\\u0070\\u0073\\u0075\\u0020\\ud83d\\ude00</body>");
    assertEquals(Kind.ATTACHMENT, row.kind());
    assertEquals("7bdb68d2-00a0-4f9d-b966-f846a1d5bec8", row.attachmentId());
    assertEquals("1x1", row.area());
    assertEquals("lor emipsu \uD83D\uDE00", row.text());
  }

  @Test
  @DisplayName("Header-path caption is decoded like any caption, entities first")
  void attachmentHeaderCaption() {
    ParsedRow row =
        attachment(
            "", "<body>lore1\nmips2 'loremi' &lt;lor&gt; &amp; \\u00e8 \\ud83d\\ude00</body>");
    assertEquals("lore1\nmips2 'loremi' <lor> & \u00e8 \uD83D\uDE00", row.text());
    assertNull(row.area());
  }

  @Test
  @DisplayName("Escaped backslash in a caption and escaped controls")
  void attachmentCaptionEdges() {
    assertEquals(
        "L:\\orem\\u0041 lor \\",
        attachment("", "<body>L:\\\\orem\\\\u0041 lor \\\\</body>").text());
    assertEquals(
        "lore mip-sum dol\u007f ors",
        attachment("", "<body>lore \\u0001mip-sum dol\\u007f \\u0000ors</body>").text());
    assertEquals("   ", attachment("", "<body>\\u0020\\u0020\\u0020</body>").text());
    assertEquals(
        "lor emip2 \u1f60" + "0 \u00e9",
        attachment("", "<body>lor emip2 \\u1f600 \\u00e9</body>").text());
  }

  @Test
  @DisplayName("Attachment without caption or with empty body has empty text")
  void attachmentWithoutCaption() {
    assertEquals("", attachment("", "").text());
    assertEquals("", attachment("", "<body/>").text());
    assertEquals(Kind.ATTACHMENT, attachment("", "<body/>").kind());
  }

  @Test
  @DisplayName("Attachment reply carries the reply reference")
  void attachmentReply() {
    ParsedRow row =
        attachment("", "<body>c</body><reply id='CNA2P086KAO1' xmlns='urn:xmpp:reply:0'/>");
    assertEquals(Kind.ATTACHMENT, row.kind());
    assertEquals("CNA2P086KAO1", row.replyRef());
  }

  @Test
  @DisplayName("Forwarded attachment is a forward with the attachment fields")
  void forwardAttachment() {
    ParsedRow row =
        parse(
            "<message"
                + " from='b4ee4ff8-9d81-4f3c-909d-0832d2fb9ca4@muclight.carbonio/69e94bcc-f52d-550d-bee1-b40dc0368d0f@carbonio'"
                + " id='3c2d3057b9b7215c' type='groupchat'><x"
                + " xmlns='urn:xmpp:muclight:0#configuration'><operation>attachmentAdded</operation>"
                + "<attachment-id>9f1c9c65-e14a-43bf-aaf4-397356b1560a</attachment-id><filename"
                + " encoded='UTF-8'>lorem.ips</filename><mime-type>image/png</mime-type><size>593</size></x><body/><forwarded"
                + " count='1' xmlns='urn:xmpp:forward:0'><delay stamp='2026-10-07T17:30:53.999763Z'"
                + " xmlns='urn:xmpp:delay'/><message"
                + " from='ec84f534-92bd-4d88-923f-ab5f8e027526@muclight.carbonio/69e94bcc-f52d-550d-bee1-b40dc0368d0f@carbonio'"
                + " id='34a5874f' type='groupchat'><body encoded='UTF-8'>lor emi"
                + " psumd</body><x><item affiliation='member'"
                + " jid='69e94bcc-f52d-550d-bee1-b40dc0368d0f@carbonio'"
                + " role='participant'/></x></message></forwarded></message>");
    assertEquals(Kind.FORWARD, row.kind());
    assertEquals("9f1c9c65-e14a-43bf-aaf4-397356b1560a", row.attachmentId());
    assertEquals("69e94bcc-f52d-550d-bee1-b40dc0368d0f", row.forwardFrom());
    assertEquals(Instant.parse("2026-10-07T17:30:53.999763Z"), row.forwardStamp());
    assertEquals("lor emi psumd", row.text());
  }

  @Test
  @DisplayName("Forwarded attachment with an empty inner body is still a forward")
  void forwardAttachmentEmptyInner() {
    ParsedRow row =
        attachment(
            "",
            "<body/><forwarded xmlns='urn:xmpp:forward:0'><message"
                + " from='97c353af-4374-5ff3-a3eb-3056c0b4dba9@carbonio'><body/></message></forwarded>");
    assertEquals(Kind.FORWARD, row.kind());
    assertEquals("", row.text());
    assertEquals("7bdb68d2-00a0-4f9d-b966-f846a1d5bec8", row.attachmentId());
  }

  private static ParsedRow config(String operation, String payload) {
    return parse(
        "<message"
            + " from='f3f01bc3-0de5-4785-acc6-7858551460fb@muclight.carbonio/1e49bba9-89c2-5a09-8eeb-efe969b5389d@carbonio'"
            + " id='f500f62ebedc27d0' type='groupchat'><x"
            + " xmlns='urn:xmpp:muclight:0#configuration'><operation>"
            + operation
            + "</operation>"
            + payload
            + "</x><body/><x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member'"
            + " jid='1e49bba9-89c2-5a09-8eeb-efe969b5389d@carbonio'"
            + " role='participant'/></x></message>");
  }

  @Test
  @DisplayName("memberAdded and memberRemoved carry the user id")
  void memberRows() {
    ParsedRow added =
        config("memberAdded", "<user-id>5c89638f-9a69-5c9b-8fa6-b4b64e10351e</user-id>");
    ParsedRow removed =
        config("memberRemoved", "<user-id>e7a245ba-f1e6-5f32-ba77-90798e7f219b</user-id>");
    assertEquals(Kind.MEMBER_ADDED, added.kind());
    assertEquals("5c89638f-9a69-5c9b-8fa6-b4b64e10351e", added.configUserId());
    assertEquals(Kind.MEMBER_REMOVED, removed.kind());
    assertEquals("e7a245ba-f1e6-5f32-ba77-90798e7f219b", removed.configUserId());
    assertEquals("f500f62ebedc27d0", added.rootId());
  }

  @Test
  @DisplayName("messagePinned carries the pin message reference")
  void pinned() {
    ParsedRow row =
        config(
            "messagePinned",
            "<pin message-id='CNA3EM62QJO1'"
                + " xmlns='zextras:iq:pin'><pinned-by>7d03b0c9-cd6e-5392-9186-1b9646f07fef@carbonio/1791-394548-990395-7e54d04434d17629</pinned-by>"
                + "<room-id>e0df7f4a-7a19-4ab7-9af7-49be3b7874b6@muclight.carbonio</room-id>"
                + "<pinned-at>1791394566055982</pinned-at><body></body></pin>");
    assertEquals(Kind.PINNED, row.kind());
    assertEquals("CNA3EM62QJO1", row.pinRef());
    assertEquals("", row.text());
  }

  @Test
  @DisplayName("messageUnpinned carries the pin message reference")
  void unpinned() {
    ParsedRow row =
        config("messageUnpinned", "<pin message-id='CNA3EJUJV581' xmlns='zextras:iq:pin'/>");
    assertEquals(Kind.UNPINNED, row.kind());
    assertEquals("CNA3EJUJV581", row.pinRef());
  }

  @Test
  @DisplayName("roomHistoryCleared is a history cleared event")
  void historyCleared() {
    ParsedRow row =
        config(
            "roomHistoryCleared",
            "<user-id>69e94bcc-f52d-550d-bee1-b40dc0368d0f</user-id><cleared-at>2026-10-08T14:05:29.989845014Z</cleared-at>");
    assertEquals(Kind.ROOM_HISTORY_CLEARED, row.kind());
  }

  @Test
  @DisplayName("Room name and description changes carry the value")
  void nameAndDescriptionChanged() {
    ParsedRow name =
        config("roomNameChanged", "<value encoded='UTF-8'>L1 &lt;lor&gt; &apos;l&apos;</value>");
    assertEquals(Kind.ROOM_NAME_CHANGED, name.kind());
    assertEquals("L1 <lor> 'l'", name.text());
    ParsedRow description =
        config("roomDescriptionChanged", "<value encoded='UTF-8'>lorem</value>");
    assertEquals(Kind.ROOM_DESCRIPTION_CHANGED, description.kind());
    assertEquals("lorem", description.text());
    ParsedRow cleared = config("roomDescriptionChanged", "<value/>");
    assertEquals(Kind.ROOM_DESCRIPTION_CHANGED, cleared.kind());
    assertEquals("", cleared.text());
  }

  @Test
  @DisplayName("Escaped name and description values are decoded like captions")
  void escapedNameAndDescription() {
    assertEquals(
        "Lor \u00e8 \uD83D\uDE00",
        config(
                "roomNameChanged",
                "<value encoded='UTF-8'>\\u004c\\u006f\\u0072 \\u00e8 \\ud83d\\ude00</value>")
            .text());
    assertEquals(
        "lore \u00e8",
        config("roomDescriptionChanged", "<value encoded='UTF-8'>lore \\u00e8</value>").text());
  }

  @Test
  @DisplayName("Room picture updated and deleted are classified")
  void pictureChanged() {
    ParsedRow updated =
        config(
            "roomPictureUpdated",
            "<picture-id>af55e141-9163-4990-89c4-383987551573</picture-id><picture-name"
                + " encoded='UTF-8'>a.png</picture-name>");
    assertEquals(Kind.ROOM_PICTURE_UPDATED, updated.kind());
    assertEquals(Kind.ROOM_PICTURE_DELETED, config("roomPictureDeleted", "").kind());
  }

  @Test
  @DisplayName("Unknown configuration operations are skipped")
  void otherConfigOperations() {
    assertEquals(Kind.SKIP, config("somethingNew", "").kind());
    assertEquals(Kind.SKIP, config("", "").kind());
  }

  @Test
  @DisplayName("meetingStarted and meetingDeclined configuration rows are classified")
  void meetingStartedAndDeclined() {
    ParsedRow started = config("meetingStarted", "<timestamp>1791394624541</timestamp>");
    ParsedRow declined = config("meetingDeclined", "<timestamp>1791394624541</timestamp>");
    assertEquals(Kind.MEETING_STARTED, started.kind());
    assertEquals(Kind.MEETING_DECLINED, declined.kind());
    assertNull(started.durationSec());
  }

  @Test
  @DisplayName("meetingEnded carries duration and startedAt")
  void meetingEnded() {
    ParsedRow row =
        config(
            "meetingEnded",
            "<timestamp>1791394724541</timestamp><startedAt>1791394624541</startedAt>"
                + "<duration>100</duration>");
    assertEquals(Kind.MEETING_ENDED, row.kind());
    assertEquals(100L, row.durationSec());
    assertEquals(1791394624541L, row.startedAtMillis());
  }

  @Test
  @DisplayName("meetingEnded with missing or invalid numbers has null values")
  void meetingEndedInvalidNumbers() {
    ParsedRow row = config("meetingEnded", "<startedAt>x</startedAt><duration>-5</duration>");
    assertEquals(Kind.MEETING_ENDED, row.kind());
    assertNull(row.durationSec());
    assertNull(row.startedAtMillis());
    assertNull(config("meetingEnded", "").durationSec());
  }

  @Test
  @DisplayName("Affiliations stanza without body is skipped")
  void affiliations() {
    assertEquals(
        Kind.SKIP,
        parse(
                "<message from='f3f01bc3-0de5-4785-acc6-7858551460fb@muclight.carbonio' id='a'"
                    + " type='groupchat'><x xmlns='urn:xmpp:muclight:0#affiliations'><user"
                    + " affiliation='member'>5c89638f-9a69-5c9b-8fa6-b4b64e10351e@carbonio</user></x>"
                    + "<body/></message>")
            .kind());
  }

  @Test
  @DisplayName("Missing root id is null")
  void missingRootId() {
    ParsedRow row = parse("<message type='groupchat'><body>x</body></message>");
    assertEquals(Kind.TEXT, row.kind());
    assertNull(row.rootId());
  }

  @Test
  @DisplayName("Deeply nested body with 1 MB of text parses in memory proportional to the input")
  void deepNestingWithLargeText() {
    int depth = 20_000;
    String chunk = "x".repeat(1_000_000 / depth);
    StringBuilder body = new StringBuilder("<body>");
    for (int i = 0; i < depth; i++) {
      body.append("<a>").append(chunk);
    }
    for (int i = 0; i < depth; i++) {
      body.append("</a>");
    }
    body.append("</body>");
    ParsedRow row = inStanza(body.toString());
    assertEquals(Kind.TEXT, row.kind());
    assertEquals(chunk.length() * depth, row.text().length());
  }

  @Test
  @DisplayName("Billion laughs does not expand")
  void billionLaughs() {
    StringBuilder dtd = new StringBuilder("<!ENTITY l0 'lol'>");
    for (int i = 1; i <= 9; i++) {
      String prev = "&l" + (i - 1) + ";";
      dtd.append("<!ENTITY l").append(i).append(" '").append(prev.repeat(10)).append("'>");
    }
    ParsedRow row =
        parse(
            "<!DOCTYPE message ["
                + dtd
                + "]><message id='a' type='groupchat'><body>&l9;</body></message>");
    assertEquals(Kind.SKIP, row.kind());
  }

  @Test
  @DisplayName("Tombstone with a missing or garbage stamp keeps a null stamp")
  void tombstoneWithoutValidStamp() {
    ParsedRow missing = inStanza("<retracted xmlns='urn:esl:message-retract-by-stanza-id:0'/>");
    ParsedRow garbage =
        inStanza("<retracted stamp='yesterday' xmlns='urn:xmpp:message-retract:0'/>");
    assertEquals(Kind.TOMBSTONE, missing.kind());
    assertNull(missing.retractedStamp());
    assertEquals(Kind.TOMBSTONE, garbage.kind());
    assertNull(garbage.retractedStamp());
  }

  @Test
  @DisplayName("A foreign retracted element does not hide the tombstone")
  void foreignRetractedIsIgnored() {
    ParsedRow row =
        inStanza(
            "<retracted stamp='1999-01-01T00:00:00Z' xmlns='urn:example:other'/>"
                + "<retracted stamp='2026-10-07T16:41:01Z' xmlns='urn:xmpp:message-retract:0'/>");
    assertEquals(Kind.TOMBSTONE, row.kind());
    assertEquals(Instant.parse("2026-10-07T16:41:01Z"), row.retractedStamp());
    assertEquals(
        Kind.SKIP,
        inStanza("<retracted stamp='2026-10-07T16:41:01Z' xmlns='urn:example:other'/>").kind());
  }

  @Test
  @DisplayName("32 MiB of empty sibling elements: the body is kept, memory stays bounded")
  void millionsOfSiblingElements() {
    byte[] row =
        largestRow("<message " + FROM + " id='siblings'><body>kept</body>", "<a/>", "</message>");
    ParsedRow[] parsed = new ParsedRow[1];
    long allocated = allocatedBy(() -> parsed[0] = StanzaParser.parse(row));
    assertEquals(Kind.TEXT, parsed[0].kind());
    assertEquals("kept", parsed[0].text());
    assertTrue(allocated < ALLOCATION_BUDGET, "allocated " + allocated + " bytes");
  }

  @Test
  @DisplayName("32 MiB nested millions of levels deep: the row is skipped, memory stays bounded")
  void millionsOfNestedElements() {
    StringBuilder xml = new StringBuilder(MAX_ROW_BYTES);
    String head = "<message " + FROM + " id='deep'><body>kept</body>";
    String tail = "</message>";
    int depth = (MAX_ROW_BYTES - head.length() - tail.length()) / 7;
    xml.append(head).append("<a>".repeat(depth)).append("</a>".repeat(depth)).append(tail);
    byte[] row = xml.toString().getBytes(StandardCharsets.UTF_8);
    ParsedRow[] parsed = new ParsedRow[1];
    long allocated = allocatedBy(() -> parsed[0] = StanzaParser.parse(row));
    assertEquals(Kind.SKIP, parsed[0].kind());
    assertTrue(allocated < ALLOCATION_BUDGET, "allocated " + allocated + " bytes");
  }
}
