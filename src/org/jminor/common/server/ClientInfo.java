/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server;

import org.jminor.common.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.UUID;

/**
 * Contains basic information about a remote client.
 */
public final class ClientInfo implements Serializable {

  private static final long serialVersionUID = 1;

  private UUID clientID;
  private String clientTypeID;
  private User user;
  private String clientHost = "unknown host";

  /**
   * Instantiates a new ClientInfo
   * @param clientID the client ID
   */
  public ClientInfo(final UUID clientID) {
    this(clientID, null, null);
  }

  /**
   * Instantiates a new ClientInfo
   * @param clientID the client ID
   * @param clientTypeID a string specifying the client type
   * @param user the user
   */
  public ClientInfo(final UUID clientID, final String clientTypeID, final User user) {
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    this.user = user;
  }

  /**
   * @return the user
   */
  public User getUser() {
    return user;
  }

  /**
   * @return the client ID
   */
  public UUID getClientID() {
    return clientID;
  }

  /**
   * @return the client type ID
   */
  public String getClientTypeID() {
    return clientTypeID;
  }

  /**
   * @return the client hostname
   */
  public String getClientHost() {
    return clientHost;
  }

  /**
   * @param clientHost the client hostname
   */
  public void setClientHost(final String clientHost) {
    this.clientHost = clientHost;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return clientID.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof ClientInfo && clientID.equals(((ClientInfo) obj).clientID);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return user != null ? user + "@" + clientHost + " [" + clientTypeID + "] - " + clientID : clientID.toString();
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(clientID);
    stream.writeObject(clientTypeID);
    stream.writeObject(user);
    stream.writeObject(clientHost);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.clientID = (UUID) stream.readObject();
    this.clientTypeID = (String) stream.readObject();
    this.user = (User) stream.readObject();
    this.clientHost = (String) stream.readObject();
  }
}