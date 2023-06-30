// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.ebeaninternal.server.util.Str;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

public class XmppMessageBuilder {

  private final String                            to;
  private final String                            from;
  private final List<SimpleEntry<String, String>> configurations;
  private       String                            messageId;
  private       String                            body;
  private       String                            replyId;
  private       String                            messageToForward;
  private       OffsetDateTime                    messageToForwardSentAt;

  public XmppMessageBuilder(String to, String from) {
    this.to = to;
    this.from = from;
    this.configurations = new ArrayList<>();
  }

  public static XmppMessageBuilder create(String to, String from) {
    return new XmppMessageBuilder(to, from);
  }

  public XmppMessageBuilder messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public XmppMessageBuilder body(String body) {
    this.body = body;
    return this;
  }

  public XmppMessageBuilder replyId(String replyId) {
    this.replyId = replyId;
    return this;
  }

  public XmppMessageBuilder messageToForward(String messageToForward) {
    this.messageToForward = messageToForward;
    return this;
  }

  public XmppMessageBuilder messageToForwardSentAt(OffsetDateTime messageToForwardSentAt) {
    this.messageToForwardSentAt = messageToForwardSentAt;
    return this;
  }

  public XmppMessageBuilder type(MessageType type) {
    configurations.add(0, new SimpleEntry<>("operation", type.getName()));
    return this;
  }

  public XmppMessageBuilder addConfig(String name, String value) {
    configurations.add(new SimpleEntry<>(name, value));
    return this;
  }

  public String build() {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element message = createMessageElement(document);
      document.appendChild(message);
      if (!configurations.isEmpty()) {
        message.appendChild(createConfigurationsElement(document));
      }
      message.appendChild(createTextElement(document, "body", Optional.ofNullable(body).orElse("")));
      Optional.ofNullable(replyId).ifPresent(id -> message.appendChild(createReplyElement(document)));
      Optional.ofNullable(messageToForward).ifPresent(m -> message.appendChild(createForwardedElement(document)));
      DOMImplementationLS domImplLS = (DOMImplementationLS) document.getImplementation();
      LSSerializer serializer = domImplLS.createLSSerializer();
      serializer.getDomConfig().setParameter("xml-declaration", false);
      LSOutput lsOutput = domImplLS.createLSOutput();
      lsOutput.setEncoding("UTF-8");
      Writer stringWriter = new StringWriter();
      lsOutput.setCharacterStream(stringWriter);
      serializer.write(document, lsOutput);
      return escapeBodyContent(stringWriter.toString().replace("\"", "'"));
    } catch (ParserConfigurationException e) {
      throw new InternalErrorException("Unable to initialize the XMPP message", e);
    }
  }

  private String escapeBodyContent(String message) {
    StringBuilder sb = new StringBuilder();
    String[] tokens = message.split("<body>");
    if (tokens.length > 0) {
      sb.append(tokens[0]);
      for (int i = 1; i < tokens.length; i++) {
        String body = tokens[i].substring(0, tokens[i].indexOf("</body>"));
        String newBody = StringEscapeUtils.escapeHtml4(StringEscapeUtils.unescapeXml(body));
        sb.append("<body>").append(tokens[i].replace(body, newBody));
      }
    } else {
      return message;
    }
    return sb.toString();
  }

  private Element createForwardedElement(Document document) {
    Element element = document.createElementNS("urn:xmpp:forward:0", "forwarded");
    Optional.ofNullable(messageToForwardSentAt).ifPresent(sentAt -> {
      Element delay = document.createElementNS("urn:xmpp:delay", "delay");
      delay.setAttribute("stamp", sentAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      element.appendChild(delay);
    });
    if (messageToForward.contains("<body>")) {
      String body = messageToForward.substring(messageToForward.indexOf("<body>") + 6,
        messageToForward.indexOf("</body>"));
      messageToForward = messageToForward.replace(body, StringEscapeUtils.escapeXml10(body));
    }
    Document documentTag;
    Element messageTag;
    try {
      documentTag = DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(messageToForward.getBytes()));
    } catch (Exception e) {
      throw new BadRequestException("Cannot read the message to forward", e);
    }
    messageTag = documentTag.getDocumentElement();
    Optional<Element> forwardedTag = Optional.ofNullable(
      Optional.ofNullable((Element) messageTag.getElementsByTagName("forwarded").item(0))
        .orElse(Optional.ofNullable((Element) messageTag.getElementsByTagName("result").item(0))
          .map(r -> (Element) r.getElementsByTagName("forwarded").item(0)).orElse(null)));
    int countAtt;
    String textBody;
    if (forwardedTag.isPresent()) {
      countAtt = forwardedTag
        .flatMap(f -> Optional.ofNullable(f.getAttributes().getNamedItem("count"))
          .map(c -> Integer.parseInt(c.getNodeValue()) + 1)).orElse(1);
      textBody = forwardedTag
        .flatMap(f -> Optional.ofNullable((Element) f.getElementsByTagName("message").item(0))
          .flatMap(m -> Optional.ofNullable(m.getElementsByTagName("body").item(0))
            .map(Node::getTextContent))).orElse("");
    } else {
      countAtt = 1;
      textBody = Optional.ofNullable(messageTag.getElementsByTagName("body").item(0))
        .map(Node::getTextContent).orElse("");
    }
    element.setAttribute("count", String.valueOf(countAtt));

    textBody = isEncodedForXmpp(textBody) ? textBody : encodeStringForXmpp(textBody);
    Optional<Element> bodyTag = Optional.ofNullable((Element) messageTag.getElementsByTagName("body").item(0));
    if (bodyTag.isPresent()) {
      bodyTag.get().setTextContent(textBody);
    } else {
      messageTag.appendChild(createTextElement(documentTag, "body", textBody));
    }

    Optional.ofNullable(messageTag.getElementsByTagName("markable").item(0)).ifPresent(messageTag::removeChild);
    Optional.ofNullable(messageTag.getElementsByTagName("forwarded").item(0)).ifPresent(messageTag::removeChild);
    Optional.ofNullable(messageTag.getElementsByTagName("reply").item(0)).ifPresent(messageTag::removeChild);
    Optional.ofNullable(messageTag.getElementsByTagName("stanza-id").item(0)).ifPresent(messageTag::removeChild);
    Optional.ofNullable(messageTag.getElementsByTagName("x").item(0)).ifPresent(messageTag::removeChild);
    Node messageTagImported = document.importNode(messageTag, true);
    element.appendChild(messageTagImported);
    return element;
  }

  private Element createReplyElement(Document document) {
    Element element = document.createElementNS("urn:xmpp:reply:0", "reply");
    element.setAttribute("to", to);
    element.setAttribute("id", replyId);
    return element;
  }

  private Element createMessageElement(Document document) {
    Element element = document.createElementNS("jabber:client", "message");
    element.setAttribute("to", Optional.ofNullable(to)
      .orElseThrow(() -> new InternalErrorException("Cannot create an XMPP message without recipient")));
    element.setAttribute("from", Optional.ofNullable(from)
      .orElseThrow(() -> new InternalErrorException("Cannot create an XMPP message without sender")));
    Optional.ofNullable(messageId).ifPresent(id -> element.setAttribute("id", id));
    element.setAttribute("type", "groupchat");
    return element;
  }

  private Element createConfigurationsElement(Document document) {
    Element element = document.createElementNS("urn:xmpp:muclight:0#configuration", "x");
    configurations.forEach(
      config -> element.appendChild(createTextElement(document, config.getKey(), config.getValue())));
    return element;
  }

  private Element createTextElement(Document document, String name, String value) {
    Element element = document.createElement(name);
    element.appendChild(document.createTextNode(value));
    return element;
  }

  public static String encodeStringForXmpp(String toEncode) {
    if (toEncode == null || toEncode.length() == 0) {
      return toEncode;
    }
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < toEncode.length(); i++) {
      String c = "";
      try {
        c = encodeCharForXmpp(toEncode.substring(i, i + 1));
      } catch (UnsupportedEncodingException ignored) {
      }
      result.append(c);
    }
    return result.toString();
  }

  private static String encodeCharForXmpp(String toEncode) throws UnsupportedEncodingException {
    if (toEncode == null || toEncode.length() != 1) {
      throw new UnsupportedEncodingException();
    }
    byte[] bytes = toEncode.getBytes(StandardCharsets.UTF_8);
    ArrayList<String> binaryList = new ArrayList<>();
    for (byte b : bytes) {
      binaryList.add(StringUtils.leftPad(Integer.toBinaryString(b & 0xFF), 8, "0"));
    }
    String filter = StringUtils.leftPad("0", binaryList.size() > 1 ? binaryList.size() + 1 : 0, "1");

    StringBuilder binary = new StringBuilder();
    for (String b : binaryList) {
      if (!b.startsWith(filter)) {
        ChatsLogger.warn(XmppMessageBuilder.class, String.format("Connot encode character '%s'", toEncode));
        throw new UnsupportedEncodingException(String.format("Connot encode character '%s'", toEncode));
      }
      binary.append(b.substring(filter.length()));
      filter = "10";
    }
    return String.format("\\\\u%4s", Integer.toHexString(Integer.parseInt(binary.toString(), 2)))
      .replace(' ', '0');
  }

  public static boolean isEncodedForXmpp(String text) {
    return Pattern.matches("[\\\\[uU][a-fA-F0-9]{4}]+", text);
  }
}
