package com.zextras.team.core.data.entity;

import com.zextras.team.core.model.RoomTypeDto;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ROOM", schema = "TEAM")
public class Room {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 128, nullable = false)
  private String name;

  @Column(name = "DESCRIPTION", length = 256)
  private String description;

  @Column(name = "HASH", length = 256, unique = true, nullable = false)
  private String hash;

  @Column(name = "DOMAIN", length = 256)
  private String domain;

  @Column(name = "TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private RoomTypeDto type;

  @Column(name = "PARENT_ID", length = 64)
  private String parentId;

  @Column(name = "PASSWORD", length = 256)
  private String password;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private LocalDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "room")
  private List<Subscription> subscriptions;

  public static Room create() {
    return new Room();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Room id(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Room name(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Room description(String description) {
    this.description = description;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public Room hash(String hash) {
    this.hash = hash;
    return this;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public Room domain(String domain) {
    this.domain = domain;
    return this;
  }

  public RoomTypeDto getType() {
    return type;
  }

  public void setType(RoomTypeDto type) {
    this.type = type;
  }

  public Room type(RoomTypeDto type) {
    this.type = type;
    return this;
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public Room parentId(String parentId) {
    this.parentId = parentId;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Room password(String password) {
    this.password = password;
    return this;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<Subscription> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(List<Subscription> subscriptions) {
    this.subscriptions = subscriptions;
  }

  public Room subscriptions(List<Subscription> subscriptions) {
    this.subscriptions = subscriptions;
    return this;
  }


}
