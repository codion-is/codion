/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.client;

import is.codion.common.user.User;
import is.codion.common.version.Version;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultConnectionRequest implements ConnectionRequest, Serializable {

  private static final long serialVersionUID = 1;

  private final User user;
  private final UUID clientId;
  private final String clientTypeId;
  private final Version clientVersion;
  private final Version frameworkVersion = Version.getVersion();
  private final Map<String, Object> parameters;

  private DefaultConnectionRequest(DefaultBuilder builder) {
    this.user = requireNonNull(builder.user, "user");
    this.clientId = requireNonNull(builder.clientId, "clientId");
    this.clientTypeId = requireNonNull(builder.clientTypeId, "clientTypeId");
    this.clientVersion = builder.clientVersion;
    this.parameters = builder.parameters;
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

  static final class DefaultBuilder implements Builder {

    private User user;
    private UUID clientId = UUID.randomUUID();
    private String clientTypeId;
    private Version clientVersion;
    private Map<String, Object> parameters;

    DefaultBuilder() {}

    @Override
    public Builder user(User user) {
      this.user = requireNonNull(user);
      return this;
    }

    @Override
    public Builder clientId(UUID clientId) {
      this.clientId = requireNonNull(clientId);
      return this;
    }

    @Override
    public Builder clientTypeId(String clientTypeId) {
      this.clientTypeId = requireNonNull(clientTypeId);
      return this;
    }

    @Override
    public Builder clientVersion(Version clientVersion) {
      this.clientVersion = clientVersion;
      return this;
    }

    @Override
    public Builder parameter(String key, Object value) {
      if (parameters == null) {
        parameters = new HashMap<>();
      }
      parameters.put(key, value);
      return this;
    }

    @Override
    public ConnectionRequest build() {
      return new DefaultConnectionRequest(this);
    }
  }
}
