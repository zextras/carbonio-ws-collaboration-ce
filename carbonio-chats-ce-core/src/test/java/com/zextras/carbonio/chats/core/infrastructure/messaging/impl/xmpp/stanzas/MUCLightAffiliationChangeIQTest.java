// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.stanzas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

@UnitTest
class MUCLightAffiliationChangeIQTest {

  @Nested
  @DisplayName("Get IQ child element builder tests")
  public class GetIQChildElementBuilderTest {

    @Test
    @DisplayName("Correctly encodes the MUC stanza with every parameter")
    public void getIQChildElementBuilder_testOK() throws Exception {
      String xml = new MUCLightAffiliationChangeIQ().from(JidCreate.from("sender@carbonio"))
        .to(JidCreate.from("destination@muclight.carbonio"))
        .addAffiliationChange(JidCreate.from("user1@carbonio"), MUCLightAffiliationType.MEMBER)
        .addAffiliationChange(JidCreate.from("user2@carbonio"), MUCLightAffiliationType.NONE)
        .addAffiliationChange(JidCreate.from("user3@carbonio"), MUCLightAffiliationType.OWNER)
        .id("123")
        .toXML().toString();
      assertEquals(
        "<iq xmlns='jabber:client' to='destination@muclight.carbonio' from='sender@carbonio' id='123' type='set'>"
          + "<query xmlns='urn:xmpp:muclight:0#affiliations'>"
          + "<user affiliation='member'>user1@carbonio</user>"
          + "<user affiliation='none'>user2@carbonio</user>"
          + "<user affiliation='owner'>user3@carbonio</user>"
          + "</query>"
          + "</iq>",
        xml);
    }

    @Test
    @DisplayName("Correctly encodes the MUC stanza without a from")
    public void getIQChildElementBuilder_testWithoutFrom() throws Exception {
      String xml = new MUCLightAffiliationChangeIQ()
        .to(JidCreate.from("destination@muclight.carbonio"))
        .addAffiliationChange(JidCreate.from("user1@carbonio"), MUCLightAffiliationType.MEMBER)
        .id("123")
        .toXML().toString();
      assertEquals(
        "<iq xmlns='jabber:client' to='destination@muclight.carbonio' id='123' type='set'>"
          + "<query xmlns='urn:xmpp:muclight:0#affiliations'>"
          + "<user affiliation='member'>user1@carbonio</user>"
          + "</query>"
          + "</iq>",
        xml);
    }

    @Test
    @DisplayName("Correctly encodes the MUC stanza without from and to")
    public void getIQChildElementBuilder_testWithoutFromAndTo() throws Exception {
      String xml = new MUCLightAffiliationChangeIQ()
        .addAffiliationChange(JidCreate.from("user1@carbonio"), MUCLightAffiliationType.MEMBER)
        .id("123")
        .toXML().toString();
      assertEquals(
        "<iq xmlns='jabber:client' id='123' type='set'>"
          + "<query xmlns='urn:xmpp:muclight:0#affiliations'>"
          + "<user affiliation='member'>user1@carbonio</user>"
          + "</query>"
          + "</iq>",
        xml);
    }

    @Test
    @DisplayName("Correctly encodes the MUC stanza without affiliation changes")
    public void getIQChildElementBuilder_testWithoutAffiliationChanges() throws Exception {
      String xml = new MUCLightAffiliationChangeIQ()
        .id("123")
        .toXML().toString();
      assertEquals(
        "<iq xmlns='jabber:client' id='123' type='set'>"
          + "<query xmlns='urn:xmpp:muclight:0#affiliations'>"
          + "</query>"
          + "</iq>",
        xml);
    }
  }

}