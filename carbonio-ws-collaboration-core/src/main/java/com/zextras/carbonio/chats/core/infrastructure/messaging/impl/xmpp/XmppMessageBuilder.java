// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp;

import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import io.ebeaninternal.server.util.Str;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.text.StringEscapeUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
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

      message.appendChild(createTextElement(document, "body", Optional.ofNullable(body).map(
        StringEscapeUtils::escapeHtml4).orElse("")));
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
      return stringWriter.toString().replace("\"", "'");
    } catch (ParserConfigurationException e) {
      throw new InternalErrorException("Unable to initialize the XMPP message", e);
    }
  }

  private Element createForwardedElement(Document document) {
    Element element = document.createElementNS("urn:xmpp:forward:0", "forwarded");
    Optional.ofNullable(messageToForwardSentAt).ifPresent(sentAt -> {
      Element delay = document.createElementNS("urn:xmpp:delay", "delay");
      delay.setAttribute("stamp", sentAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      element.appendChild(delay);
    });
    Element messageTag = null;
    try {
      messageTag = DocumentBuilderFactory
        .newInstance()
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(messageToForward.getBytes())).getDocumentElement();
      Element bodyElement = (Element) messageTag.getElementsByTagName("body").item(0);

      if (bodyElement != null) {
        String body = bodyElement.getTextContent();
        if (body.contains("\n")) {
          body =String.join(System.lineSeparator(), body.split("\n"));
        }
        bodyElement.setTextContent(StringEscapeUtils.escapeXml11(body));
      }
    } catch (Exception e) {
      throw new BadRequestException("Cannot read the message to forward", e);
    }
    Optional.ofNullable(messageTag.getElementsByTagName("markable").item(0)).ifPresent(messageTag::removeChild);
    Optional.ofNullable(messageTag.getElementsByTagName("forwarded").item(0)).ifPresent(messageTag::removeChild);
    Optional.ofNullable(messageTag.getElementsByTagName("reply").item(0)).ifPresent(messageTag::removeChild);
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
}
