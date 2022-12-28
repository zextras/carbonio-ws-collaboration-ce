// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.xmpp.stanzas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jivesoftware.smack.packet.IQ;
import org.jxmpp.jid.Jid;

/**
 * This is not used anymore. It serves as an example on how to create a custom stanza using Smack
 */
public class MUCLightAffiliationChangeIQ extends IQ {

  private static final String childElement          = "query";
  private static final String childElementNamespace = "urn:xmpp:muclight:0#affiliations";

  private final List<AffiliationAndUser> affiliations;

  public MUCLightAffiliationChangeIQ() {
    super(childElement, childElementNamespace);
    affiliations = new ArrayList<>();
    super.setType(Type.set);
  }

  public MUCLightAffiliationChangeIQ from(Jid from) {
    super.setFrom(from);
    return this;
  }

  public MUCLightAffiliationChangeIQ to(Jid to) {
    super.setTo(to);
    return this;
  }

  public MUCLightAffiliationChangeIQ id(String id) {
    super.setStanzaId(id);
    return this;
  }

  public MUCLightAffiliationChangeIQ addAffiliationChange(Jid member, MUCLightAffiliationType affiliationType) {
    affiliations.add(new AffiliationAndUser(member, affiliationType));
    return this;
  }

  @Override
  protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
    IQChildElementXmlStringBuilder iqChildElementXmlStringBuilder
  ) {
    iqChildElementXmlStringBuilder.rightAngleBracket();
    affiliations.forEach(affiliationAndUser -> {
      iqChildElementXmlStringBuilder.halfOpenElement("user");
      iqChildElementXmlStringBuilder.attribute("affiliation", affiliationAndUser.getAffiliationType());
      iqChildElementXmlStringBuilder.rightAngleBracket();
      iqChildElementXmlStringBuilder.escapeAttributeValue(affiliationAndUser.getMember().toString());
      iqChildElementXmlStringBuilder.closeElement("user");
    });
    return iqChildElementXmlStringBuilder;
  }

  private static class AffiliationAndUser {

    private final Jid                     member;
    private final MUCLightAffiliationType affiliationType;

    public AffiliationAndUser(
      Jid member,
      MUCLightAffiliationType affiliationType
    ) {
      this.member = member;
      this.affiliationType = affiliationType;
    }

    public Jid getMember() {
      return member;
    }

    public MUCLightAffiliationType getAffiliationType() {
      return affiliationType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      AffiliationAndUser that = (AffiliationAndUser) o;
      return Objects.equals(member, that.member) && affiliationType == that.affiliationType;
    }

    @Override
    public int hashCode() {
      return Objects.hash(member, affiliationType);
    }
  }
}
