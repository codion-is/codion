/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultRemoteClient implements RemoteClient, Serializable {

  private static final long serialVersionUID = 1;

  private final ConnectionRequest connectionRequest;
  private final User databaseUser;

  private String clientHost;

  /**
   * Instantiates a new RemoteClient
   * @param connectionRequest the connection request
   * @param databaseUser the user to use when connecting to the underlying database
   */
  DefaultRemoteClient(ConnectionRequest connectionRequest, User databaseUser) {
    this.connectionRequest = requireNonNull(connectionRequest, "connectionRequest");
    this.databaseUser = requireNonNull(databaseUser, "databaseUser");
  }

  @Override
  public ConnectionRequest getConnectionRequest() {
    return connectionRequest;
  }

  @Override
  public User getUser() {
    return connectionRequest.getUser();
  }

  @Override
  public User getDatabaseUser() {
    return databaseUser;
  }

  @Override
  public UUID getClientId() {
    return connectionRequest.getClientId();
  }

  @Override
  public String getClientTypeId() {
    return connectionRequest.getClientTypeId();
  }

  @Override
  public Version getClientVersion() {
    return connectionRequest.getClientVersion();
  }

  @Override
  public Version getFrameworkVersion() {
    return connectionRequest.getFrameworkVersion();
  }

  @Override
  public Map<String, Object> getParameters() {
    return connectionRequest.getParameters();
  }

  @Override
  public String getClientHost() {
    return clientHost;
  }

  @Override
  public void setClientHost(String clientHost) {
    this.clientHost = clientHost;
  }

  @Override
  public RemoteClient withDatabaseUser(User databaseUser) {
    RemoteClient client = new DefaultRemoteClient(connectionRequest, databaseUser);
    client.setClientHost(clientHost);

    return client;
  }

  @Override
  public int hashCode() {
    return connectionRequest.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof RemoteClient && connectionRequest.equals(((RemoteClient) obj).getConnectionRequest());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(connectionRequest.getUser().toString());
    if (databaseUser != null && !connectionRequest.getUser().equals(databaseUser)) {
      builder.append(" (databaseUser: ").append(databaseUser).append(")");
    }
    builder.append("@").append(clientHost == null ? "unknown" : clientHost).append(" [").append(connectionRequest.getClientTypeId())
            .append(connectionRequest.getClientVersion() != null ? "-" + connectionRequest.getClientVersion() : "")
            .append("] - ").append(connectionRequest.getClientId().toString());

    return builder.toString();
  }
}
