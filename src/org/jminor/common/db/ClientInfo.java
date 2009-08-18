/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
* User: Björn Darri
* Date: 11.12.2007
* Time: 13:19:13
*/
public class ClientInfo implements Serializable {

  private static final long serialVersionUID = 1;

  private final String clientID;
  private final String clientTypeID;
  private final User user;

  public ClientInfo(final String clientID) {
    this(clientID, null, null);
  }

  public ClientInfo(final String clientID, final String clientTypeID, final User user) {
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    this.user = user;
  }

  public User getUser() {
    return user;
  }

  public String getClientID() {
    return clientID;
  }

  public String getClientTypeID() {
    return clientTypeID;
  }

  @Override
  public int hashCode() {
    return getClass().hashCode() + clientID.hashCode();
  }

  @Override
  public boolean equals(final Object object) {
    return this == object || object instanceof ClientInfo && clientID.equals(((ClientInfo) object).clientID);
  }

  @Override
  public String toString() {
    return user != null ? user + " - " + clientID : clientID;
  }
}
