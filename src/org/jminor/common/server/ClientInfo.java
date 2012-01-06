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
  private User databaseUser;
  private String clientHost = "unknown";

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
    this(clientID, clientTypeID, user, user);
  }

  /**
   * Instantiates a new ClientInfo
   * @param clientID the client ID
   * @param clientTypeID a string specifying the client type
   * @param user the user
   * @param databaseUser the user to use when connecting to the underlying database
   */
  public ClientInfo(final UUID clientID, final String clientTypeID, final User user, final User databaseUser) {
    this.clientID = clientID;
    this.clientTypeID = clientTypeID;
    this.user = user;
    this.databaseUser = databaseUser;
  }

  /**
   * @return the user
   */
  public User getUser() {
    return user;
  }

  /**
   * @return the user used when connecting to the underlying database
   */
  public User getDatabaseUser() {
    return databaseUser;
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
    if (user == null) {
      return clientID.toString();
    }

    final StringBuilder builder = new StringBuilder(user.toString());
    if (databaseUser != null && !user.equals(databaseUser)) {
      builder.append(" (databaseUser: ").append(databaseUser.toString()).append(")");
    }
    builder.append("@").append(clientHost).append(" [").append(clientTypeID).append("] - ").append(clientID.toString());

    return builder.toString();
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(clientID);
    stream.writeObject(clientTypeID);
    stream.writeObject(user);
    stream.writeObject(databaseUser);
    stream.writeObject(clientHost);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    this.clientID = (UUID) stream.readObject();
    this.clientTypeID = (String) stream.readObject();
    this.user = (User) stream.readObject();
    this.databaseUser = (User) stream.readObject();
    this.clientHost = (String) stream.readObject();
  }
}