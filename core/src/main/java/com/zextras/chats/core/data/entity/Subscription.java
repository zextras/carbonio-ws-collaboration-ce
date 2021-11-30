package com.zextras.chats.core.data.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "SUBSCRIPTION", schema = "CHATS")
public class Subscription {

  @EmbeddedId
  private SubscriptionId id;

  @MapsId("userId")
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roomId")
  @JoinColumn(name = "ROOM_ID")
  private Room room;

  @Column(name = "JOINED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private Date joinedAt;

  @Column(name = "OWNER")
  private Boolean isOwner = false;

  @Column(name = "EXTERNAL")
  private Boolean isExternal = false;

  @Column(name = "TEMPORARY")
  private Boolean isTemporary = false;

  public static Subscription create() {
    return new Subscription();
  }

  public SubscriptionId getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public Room getRoom() {
    return room;
  }

  public Date getJoinedAt() {
    return joinedAt;
  }

  public Boolean getIsOwner() {
    return isOwner;
  }

  public Boolean getIsExternal() {
    return isExternal;
  }

  public Boolean getIsTemporary() {
    return isTemporary;
  }

  public Subscription id(SubscriptionId id) {
    this.id = id;
    return this;
  }

  public Subscription userId(String userId) {
    this.userId = userId;
    return this;
  }

  public Subscription room(Room room) {
    this.room = room;
    return this;
  }

  public Subscription joinedAt(Date joinedAt) {
    this.joinedAt = joinedAt;
    return this;
  }

  public Subscription owner(Boolean owner) {
    this.isOwner = owner;
    return this;
  }

  public Subscription external(Boolean external) {
    this.isExternal = external;
    return this;
  }

  public Subscription temporary(Boolean temporary) {
    this.isTemporary = temporary;
    return this;
  }
}
