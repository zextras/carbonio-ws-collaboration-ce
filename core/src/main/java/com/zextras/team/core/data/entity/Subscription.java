package com.zextras.team.core.data.entity;

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
@Table(name = "SUBSCRIPTION", schema = "TEAM")
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
  private Boolean owner = false;

  @Column(name = "EXTERNAL")
  private Boolean external = false;

  @Column(name = "TEMPORARY")
  private Boolean temporary = false;

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

  public Boolean getOwner() {
    return owner;
  }

  public Boolean getExternal() {
    return external;
  }

  public Boolean getTemporary() {
    return temporary;
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
    this.owner = owner;
    return this;
  }

  public Subscription external(Boolean external) {
    this.external = external;
    return this;
  }

  public Subscription temporary(Boolean temporary) {
    this.temporary = temporary;
    return this;
  }
}
