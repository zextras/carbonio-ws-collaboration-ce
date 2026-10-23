// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow.Kind;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public final class StanzaParser {

  private static final String NS_CONFIG = "urn:xmpp:muclight:0#configuration";
  private static final String NS_AFFILIATIONS = "urn:xmpp:muclight:0#affiliations";
  private static final String NS_RETRACT_ESL = "urn:esl:message-retract-by-stanza-id:0";
  private static final String NS_RETRACT_XEP = "urn:xmpp:message-retract:0";
  private static final String NS_FASTEN = "urn:xmpp:fasten:0";
  private static final String NS_EDIT = "zextras:xmpp:edit:0";
  private static final String NS_REACTION = "zextras:xmpp:reaction:0";
  private static final String NS_FORWARD = "urn:xmpp:forward:0";
  private static final String NS_REPLY = "urn:xmpp:reply:0";
  private static final String NS_CORRECT = "urn:xmpp:message-correct:0";
  private static final Pattern UUID =
      Pattern.compile(
          "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

  // classify reads at most four levels (message/forwarded/message/body); deeper text still counts.
  private static final int MAX_NODE_DEPTH = 4;
  private static final int MAX_NODES = 10_000;
  private static final int MAX_ELEMENT_DEPTH = 100_000;

  private static final ThreadLocal<XMLInputFactory> FACTORY =
      ThreadLocal.withInitial(
          () -> {
            XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.IS_COALESCING, true);
            factory.setProperty("jdk.xml.maxElementDepth", MAX_ELEMENT_DEPTH);
            return factory;
          });

  private StanzaParser() {}

  public static ParsedRow parse(byte[] stored) {
    if (stored == null) {
      return ParsedRow.skip();
    }
    Node root;
    try {
      root = read(XmlText.stripIllegal(new String(stored, StandardCharsets.UTF_8)));
    } catch (XMLStreamException e) {
      return ParsedRow.skip();
    }
    return root == null || !root.is("message") ? ParsedRow.skip() : classify(root);
  }

  private static ParsedRow classify(Node root) {
    String rootId = root.attr("id");

    Node retracted = root.child("retracted", NS_RETRACT_ESL);
    if (retracted == null) {
      retracted = root.child("retracted", NS_RETRACT_XEP);
    }
    if (retracted != null) {
      return row(Kind.TOMBSTONE, rootId)
          .retractedStamp(parseInstant(retracted.attr("stamp")))
          .build();
    }

    Node config = root.child("x", NS_CONFIG);
    Node forwarded = root.child("forwarded", NS_FORWARD);
    if (config != null) {
      Node operation = config.child("operation");
      String op = operation == null ? "" : operation.text().trim();
      Node userId = config.child("user-id");
      String user = userId == null ? null : userId.text().trim();
      switch (op) {
        case "memberAdded":
          return row(Kind.MEMBER_ADDED, rootId).configUserId(user).build();
        case "memberRemoved":
          return row(Kind.MEMBER_REMOVED, rootId).configUserId(user).build();
        case "messagePinned":
          return row(Kind.PINNED, rootId).configUserId(user).pinRef(pinRef(config)).build();
        case "messageUnpinned":
          return row(Kind.UNPINNED, rootId).configUserId(user).pinRef(pinRef(config)).build();
        case "attachmentAdded":
          return attachmentRow(root, rootId, config, forwarded);
        case "meetingStarted":
          return row(Kind.MEETING_STARTED, rootId).build();
        case "meetingDeclined":
          return row(Kind.MEETING_DECLINED, rootId).build();
        case "meetingEnded":
          return row(Kind.MEETING_ENDED, rootId)
              .durationSec(number(config.child("duration")))
              .startedAtMillis(number(config.child("startedAt")))
              .build();
        case "roomNameChanged":
          return row(Kind.ROOM_NAME_CHANGED, rootId).text(valueOf(config)).build();
        case "roomDescriptionChanged":
          return row(Kind.ROOM_DESCRIPTION_CHANGED, rootId).text(valueOf(config)).build();
        case "roomPictureUpdated":
          return row(Kind.ROOM_PICTURE_UPDATED, rootId).build();
        case "roomPictureDeleted":
          return row(Kind.ROOM_PICTURE_DELETED, rootId).build();
        case "roomHistoryCleared":
          return row(Kind.ROOM_HISTORY_CLEARED, rootId).build();
        default:
          return row(Kind.SKIP, rootId).configUserId(user).build();
      }
    }

    List<Node> applyTos = root.children("apply-to", NS_FASTEN);
    if (!applyTos.isEmpty()) {
      return classifyApplyTo(root, rootId, applyTos);
    }

    if (forwarded != null) {
      Node inner = forwarded.child("message");
      if (inner == null || inner.child("body") == null) {
        return row(Kind.SKIP, rootId).build();
      }
      return forwardFields(row(Kind.FORWARD, rootId), forwarded).build();
    }

    if (root.child("replace", NS_CORRECT) != null) {
      return row(Kind.SKIP, rootId).build();
    }

    Node body = root.child("body");
    if (body == null || root.child("x", NS_AFFILIATIONS) != null) {
      return row(Kind.SKIP, rootId).build();
    }
    return row(Kind.TEXT, rootId).text(body.text()).replyRef(replyRef(root)).build();
  }

  private static ParsedRow attachmentRow(Node root, String rootId, Node config, Node forwarded) {
    Builder builder = attachmentFields(row(Kind.ATTACHMENT, rootId), config);
    if (forwarded != null) {
      return forwardFields(builder.kind(Kind.FORWARD), forwarded).build();
    }
    Node body = root.child("body");
    return builder
        .text(body == null ? "" : XmlText.decodeUnicodeEscapes(body.text()))
        .replyRef(replyRef(root))
        .build();
  }

  private static ParsedRow classifyApplyTo(Node root, String rootId, List<Node> applyTos) {
    Node first = applyTos.get(0);
    String ref = first.attr("id");
    for (Node applyTo : applyTos) {
      for (Node child : applyTo.children) {
        if (child.is("retract") && (child.inNs(NS_RETRACT_ESL) || child.inNs(NS_RETRACT_XEP))) {
          return row(Kind.RETRACT_REQUEST, rootId).applyToRef(ref).build();
        }
      }
    }
    Node body = root.child("body");
    String text = body == null ? "" : body.text();
    if (first.child("edit", NS_EDIT) != null) {
      if (body == null) {
        return row(Kind.SKIP, rootId).applyToRef(ref).build();
      }
      return row(Kind.EDIT, rootId).applyToRef(ref).text(text).build();
    }
    if (first.child("reaction", NS_REACTION) != null) {
      return row(Kind.REACTION, rootId).applyToRef(ref).text(text).build();
    }
    return row(Kind.SKIP, rootId).applyToRef(ref).build();
  }

  private static Builder attachmentFields(Builder builder, Node config) {
    Node id = config.child("attachment-id");
    Node area = config.child("area");
    String areaText = area == null ? null : area.text();
    return builder
        .attachmentId(id == null ? null : id.text().trim())
        .area(areaText == null || areaText.isEmpty() ? null : areaText);
  }

  private static Builder forwardFields(Builder builder, Node forwarded) {
    Node inner = forwarded.child("message");
    Node delay = forwarded.child("delay");
    if (delay == null && inner != null) {
      delay = inner.child("delay");
    }
    builder.forwardStamp(delay == null ? null : parseInstant(delay.attr("stamp")));
    if (inner == null) {
      return builder.text("");
    }
    Node body = inner.child("body");
    return builder
        .forwardFrom(forwardFrom(inner.attr("from")))
        .text(body == null ? "" : XmlText.decodeUnicodeEscapes(body.text()));
  }

  private static String forwardFrom(String from) {
    if (from == null) {
      return null;
    }
    int room = from.indexOf("@muclight.carbonio/");
    String rest = room >= 0 ? from.substring(room + "@muclight.carbonio/".length()) : from;
    int at = rest.indexOf('@');
    if (at < 0) {
      return null;
    }
    String candidate = rest.substring(0, at);
    return UUID.matcher(candidate).matches() ? candidate.toLowerCase(Locale.ROOT) : null;
  }

  private static Long number(Node node) {
    if (node == null) {
      return null;
    }
    try {
      long value = Long.parseLong(node.text().trim());
      return value < 0 ? null : value;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String valueOf(Node config) {
    Node value = config.child("value");
    return value == null ? "" : XmlText.decodeUnicodeEscapes(value.text());
  }

  private static String pinRef(Node config) {
    Node pin = config.child("pin");
    return pin == null ? null : pin.attr("message-id");
  }

  private static String replyRef(Node root) {
    Node reply = root.child("reply", NS_REPLY);
    return reply == null ? null : reply.attr("id");
  }

  private static Instant parseInstant(String value) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(value.trim());
    } catch (RuntimeException e) {
      try {
        return OffsetDateTime.parse(value.trim()).toInstant();
      } catch (RuntimeException e2) {
        return null;
      }
    }
  }

  private static Builder row(Kind kind, String rootId) {
    return new Builder(kind, rootId);
  }

  private static Node read(String xml) throws XMLStreamException {
    XMLStreamReader reader = FACTORY.get().createXMLStreamReader(new StringReader(xml));
    StringBuilder chars = new StringBuilder();
    try {
      Node root = null;
      Node current = null;
      int depth = 0;
      int currentDepth = 0;
      int nodes = 0;
      while (reader.hasNext()) {
        switch (reader.next()) {
          case XMLStreamConstants.START_ELEMENT:
            depth++;
            if (depth != currentDepth + 1 || depth > MAX_NODE_DEPTH || nodes >= MAX_NODES) {
              break;
            }
            Node node = new Node(reader.getLocalName(), reader.getNamespaceURI(), current, chars);
            for (int i = 0; i < reader.getAttributeCount(); i++) {
              String attributeNs = reader.getAttributeNamespace(i);
              if (attributeNs == null || attributeNs.isEmpty()) {
                node.attrs.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
              }
            }
            if (current == null) {
              root = node;
            } else {
              current.children.add(node);
            }
            current = node;
            currentDepth = depth;
            nodes++;
            break;
          case XMLStreamConstants.END_ELEMENT:
            if (depth == currentDepth) {
              current.end = chars.length();
              current = current.parent;
              currentDepth--;
            }
            depth--;
            break;
          case XMLStreamConstants.CHARACTERS:
          case XMLStreamConstants.CDATA:
          case XMLStreamConstants.SPACE:
            if (current != null) {
              chars.append(reader.getText());
            }
            break;
          default:
            break;
        }
      }
      return root;
    } finally {
      reader.close();
    }
  }

  private static final class Node {
    final String name;
    final String ns;
    final Node parent;
    final Map<String, String> attrs = new HashMap<>();
    final List<Node> children = new ArrayList<>();
    private final StringBuilder chars;
    private final int start;
    int end;

    Node(String name, String ns, Node parent, StringBuilder chars) {
      this.name = name;
      this.ns = ns == null ? "" : ns;
      this.parent = parent;
      this.chars = chars;
      this.start = chars.length();
    }

    String text() {
      return chars.substring(start, end);
    }

    String attr(String key) {
      return attrs.get(key);
    }

    boolean is(String localName) {
      return name.equals(localName);
    }

    boolean inNs(String namespace) {
      return ns.equals(namespace);
    }

    Node child(String localName) {
      for (Node c : children) {
        if (c.is(localName)) {
          return c;
        }
      }
      return null;
    }

    Node child(String localName, String namespace) {
      for (Node c : children) {
        if (c.is(localName) && c.inNs(namespace)) {
          return c;
        }
      }
      return null;
    }

    List<Node> children(String localName, String namespace) {
      List<Node> out = new ArrayList<>();
      for (Node c : children) {
        if (c.is(localName) && c.inNs(namespace)) {
          out.add(c);
        }
      }
      return out;
    }
  }

  private static final class Builder {
    private Kind kind;
    private final String rootId;
    private String text = "";
    private String replyRef;
    private String applyToRef;
    private Instant retractedStamp;
    private String attachmentId;
    private String area;
    private String forwardFrom;
    private Instant forwardStamp;
    private String configUserId;
    private String pinRef;
    private Long durationSec;
    private Long startedAtMillis;

    Builder(Kind kind, String rootId) {
      this.kind = kind;
      this.rootId = rootId;
    }

    Builder kind(Kind value) {
      kind = value;
      return this;
    }

    Builder text(String value) {
      text = value;
      return this;
    }

    Builder replyRef(String value) {
      replyRef = value;
      return this;
    }

    Builder applyToRef(String value) {
      applyToRef = value;
      return this;
    }

    Builder retractedStamp(Instant value) {
      retractedStamp = value;
      return this;
    }

    Builder attachmentId(String value) {
      attachmentId = value;
      return this;
    }

    Builder area(String value) {
      area = value;
      return this;
    }

    Builder forwardFrom(String value) {
      forwardFrom = value;
      return this;
    }

    Builder forwardStamp(Instant value) {
      forwardStamp = value;
      return this;
    }

    Builder configUserId(String value) {
      configUserId = value;
      return this;
    }

    Builder pinRef(String value) {
      pinRef = value;
      return this;
    }

    Builder durationSec(Long value) {
      durationSec = value;
      return this;
    }

    Builder startedAtMillis(Long value) {
      startedAtMillis = value;
      return this;
    }

    ParsedRow build() {
      return new ParsedRow(
          kind,
          rootId,
          text,
          replyRef,
          applyToRef,
          retractedStamp,
          attachmentId,
          area,
          forwardFrom,
          forwardStamp,
          configUserId,
          pinRef,
          durationSec,
          startedAtMillis);
    }
  }
}
