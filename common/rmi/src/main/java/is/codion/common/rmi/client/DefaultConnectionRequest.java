/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultConnectionRequest implements ConnectionRequest, Serializable {

  private static final long serialVersionUID = 1;

  private final User user;
  private final UUID clientId;
  private final String clientTypeId;
  private final Version clientVersion;
  private final Version frameworkVersion;
  private final Map<String, Object> parameters;

  DefaultConnectionRequest(User user, UUID clientId, String clientTypeId,
                           Version clientVersion, Version frameworkVersion,
                           Map<String, Object> parameters) {
    this.user = requireNonNull(user, "user");
    this.clientId = requireNonNull(clientId, "clientId");
    this.clientTypeId = requireNonNull(clientTypeId, "clientTypeId");
    this.clientVersion = clientVersion;
    this.frameworkVersion = frameworkVersion;
    this.parameters = parameters;
  }

  @Override
  public User getUser() {
    return user;
  }

  @Override
  public UUID getClientId() {
    return clientId;
  }

  @Override
  public String getClientTypeId() {
    return clientTypeId;
  }

  @Override
  public Version getClientVersion() {
    return clientVersion;
  }

  @Override
  public Version getFrameworkVersion() {
    return frameworkVersion;
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters == null ? Collections.emptyMap() : parameters;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof ConnectionRequest && clientId.equals(((ConnectionRequest) obj).getClientId());
  }

  @Override
  public int hashCode() {
    return clientId.hashCode();
  }

  @Override
  public String toString() {
    return user.toString() + " [" + clientTypeId + "] - " + clientId.toString();
  }
}
