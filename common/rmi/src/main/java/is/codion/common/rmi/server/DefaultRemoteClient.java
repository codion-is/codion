/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.rmi.client.ConnectionRequest;
import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultRemoteClient implements RemoteClient, Serializable {

  private static final long serialVersionUID = 1;

  private final ConnectionRequest connectionRequest;
  private final User databaseUser;
  private final String clientHost;

  DefaultRemoteClient(ConnectionRequest connectionRequest, User databaseUser, String clientHost) {
    this.connectionRequest = requireNonNull(connectionRequest, "connectionRequest");
    this.databaseUser = requireNonNull(databaseUser, "databaseUser");
    this.clientHost = clientHost;
  }

  @Override
  public ConnectionRequest connectionRequest() {
    return connectionRequest;
  }

  @Override
  public User user() {
    return connectionRequest.user();
  }

  @Override
  public User databaseUser() {
    return databaseUser;
  }

  @Override
  public UUID clientId() {
    return connectionRequest.clientId();
  }

  @Override
  public String clientTypeId() {
    return connectionRequest.clientTypeId();
  }

  @Override
  public Locale clientLocale() {
    return connectionRequest.clientLocale();
  }

  @Override
  public ZoneId clientTimeZone() {
    return connectionRequest.clientTimeZone();
  }

  @Override
  public Version clientVersion() {
    return connectionRequest.clientVersion();
  }

  @Override
  public Version frameworkVersion() {
    return connectionRequest.frameworkVersion();
  }

  @Override
  public Map<String, Object> parameters() {
    return connectionRequest.parameters();
  }

  @Override
  public String clientHost() {
    return clientHost;
  }

  @Override
  public RemoteClient withDatabaseUser(User databaseUser) {
    return new DefaultRemoteClient(connectionRequest, databaseUser, clientHost);
  }

  @Override
  public RemoteClient copy() {
    return new DefaultRemoteClient(connectionRequest.copy(), databaseUser.copy(), clientHost);
  }

  @Override
  public int hashCode() {
    return connectionRequest.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof RemoteClient && connectionRequest.equals(((RemoteClient) obj).connectionRequest());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder(connectionRequest.user().toString());
    if (databaseUser != null && !connectionRequest.user().equals(databaseUser)) {
      builder.append(" (databaseUser: ").append(databaseUser).append(")");
    }
    builder.append("@").append(clientHost == null ? "unknown" : clientHost).append(" [").append(connectionRequest.clientTypeId())
            .append(connectionRequest.clientVersion() != null ? "-" + connectionRequest.clientVersion() : "")
            .append("] - ").append(connectionRequest.clientId().toString());

    return builder.toString();
  }
}
