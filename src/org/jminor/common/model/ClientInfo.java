/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.db.User;

import java.io.Serializable;

/**
* User: Bj�rn Darri
* Date: 11.12.2007
* Time: 13:19:13
*/
public class ClientInfo implements Serializable {

  final String clientID;
  final String clientTypeID;
  final User user;

  public ClientInfo(final String clientID) {
    this.clientID = clientID;
    this.clientTypeID = null;
    this.user = null;
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

  public int hashCode() {
    return getClass().hashCode() + clientID.hashCode();
  }

  public boolean equals(Object obj) {
    return obj instanceof ClientInfo && clientID.equals(((ClientInfo) obj).clientID);
  }

  public String toString() {
    return user != null ? user + " - " + clientID : clientID;
  }
}
