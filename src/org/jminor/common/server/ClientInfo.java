/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;

import java.io.Serializable;
import java.util.UUID;

/**
 * Contains basic information about a remote client.<br>
 * User: Bjorn Darri<br>
 * Date: 11.12.2007<br>
 * Time: 13:19:13<br>
 */
public final class ClientInfo implements Serializable {

  private static final long serialVersionUID = 1;

  private final UUID clientID;
  private final String clientTypeID;
  private final User user;
  private String clientHost = "unknown host";

  public ClientInfo(final UUID clientID) {
    this(clientID, null, null);
  }

  public ClientInfo(final UUID clientID, final String clientTypeID, final User user) {
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public UUID getClientID() {
    return clientID;
  }

  public String getClientTypeID() {
    return clientTypeID;
  }

  public String getClientHost() {
    return clientHost;
  }

  public void setClientHost(final String clientHost) {
    this.clientHost = clientHost;
  }

  @Override
  public int hashCode() {
    return clientID.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof ClientInfo && clientID.equals(((ClientInfo) obj).clientID);
  }

  @Override
  public String toString() {
    return user != null ? user + "@" + clientHost + " [" + clientTypeID + "] - " + clientID : clientID.toString();
  }
}
