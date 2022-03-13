/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.properties.PropertyValue;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.domain.entity.Entities;

import java.util.ServiceLoader;
import java.util.UUID;

/**
 * Specifies a class responsible for providing a single {@link EntityConnection} instance.
 * {@link #getConnection()} is guaranteed to return a healthy connection or throw an exception.
 * A factory class for handing out EntityConnectionProviders according to system properties.
 */
public interface EntityConnectionProvider extends AutoCloseable {

  /**
   * Indicates a local database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  String CONNECTION_TYPE_LOCAL = "local";

  /**
   * Indicates a remote database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  String CONNECTION_TYPE_REMOTE = "remote";

  /**
   * Indicates a http database connection
   * @see #CLIENT_CONNECTION_TYPE
   */
  String CONNECTION_TYPE_HTTP = "http";

  /**
   * Specifies the name of the domain model class required for a client connection.<br>
   * Value type: String<br>
   * Default value: null
   */
  PropertyValue<String> CLIENT_DOMAIN_CLASS = Configuration.stringValue("codion.client.domainClass")
          .build();

  /**
   * Specifies whether the client should connect locally, via rmi or http,
   * accepted values: local, remote, http<br>
   * Value type: String<br>
   * Default value: local
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   * @see #CONNECTION_TYPE_HTTP
   */
  PropertyValue<String> CLIENT_CONNECTION_TYPE = Configuration.stringValue("codion.client.connectionType")
          .defaultValue(CONNECTION_TYPE_LOCAL)
          .build();

  /**
   * Returns the domain entities this connection is based on
   * @return the underlying domain entities
   */
  Entities getEntities();

  /**
   * Provides a EntityConnection object, is responsible for returning a healthy EntityConnection object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return a EntityConnection instance
   */
  EntityConnection getConnection();

  /**
   * Returns a String specifying the type of connection provided by this connection provider
   * @return a String specifying the type of connection, e.g. "local" or "remote"
   */
  String getConnectionType();

  /**
   * @return a short description of the database provider
   */
  String getDescription();

  /**
   * @return true if a connection has been established, note that this does not check if the actual
   * connection is valid, only that one has been established.
   * @see #isConnectionValid()
   */
  boolean isConnected();

  /**
   * @return true if a connection has been establised and the connection is in a valid state
   */
  boolean isConnectionValid();

  /**
   * Adds a listener notified each time this connection provider establishes a connection to the database
   * @param listener a listener notified when a connection is established
   */
  void addOnConnectListener(EventDataListener<EntityConnection> listener);

  /**
   * Removes the given listener
   * @param listener the listener to remove
   */
  void removeOnConnectListener(EventDataListener<EntityConnection> listener);

  /**
   * Logs out, disconnects and performs cleanup if required
   */
  void close();

  /**
   * Disconnects the underlying connection if connected.
   * @param user the user
   * @return this EntityConnectionProvider instance
   */
  EntityConnectionProvider setUser(User user);

  /**
   * @return the user used by this connection provider
   */
  User getUser();

  /**
   * Disconnects the underlying connection if connected.
   * @param domainClassName the name of the class specifying the domain model for this connection provider
   * @return this EntityConnectionProvider instance
   * @throws IllegalArgumentException in case {@code domainClassName} is null
   */
  EntityConnectionProvider setDomainClassName(String domainClassName);

  /**
   * @return the domain model classname
   */
  String getDomainClassName();

  /**
   * Disconnects the underlying connection if connected.
   * @param clientId the UUID identifying this client connection
   * @return this EntityConnectionProvider instance
   * @throws IllegalArgumentException in case {@code clientId} is null
   */
  EntityConnectionProvider setClientId(UUID clientId);

  /**
   * @return the UUID identifying this client connection
   */
  UUID getClientId();

  /**
   * Disconnects the underlying connection if connected.
   * @param clientTypeId a String identifying the client type for this connection provider
   * @return this EntityConnectionProvider instance
   */
  EntityConnectionProvider setClientTypeId(String clientTypeId);

  /**
   * @return the String identifying the client type for this connection provider
   */
  String getClientTypeId();

  /**
   * @param clientVersion the client version
   * @return this EntityConnectionProvider instance
   */
  EntityConnectionProvider setClientVersion(Version clientVersion);

  /**
   * @return the client version
   */
  Version getClientVersion();

  /**
   * @return a unconfigured {@link EntityConnectionProvider} instance,
   * based on {@link EntityConnectionProvider#CLIENT_CONNECTION_TYPE} configuration value
   * @see EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   */
  static EntityConnectionProvider connectionProvider() {
    String clientConnectionType = CLIENT_CONNECTION_TYPE.getOrThrow();
    ServiceLoader<EntityConnectionProvider> loader = ServiceLoader.load(EntityConnectionProvider.class);
    for (EntityConnectionProvider provider : loader) {
      if (provider.getConnectionType().equalsIgnoreCase(clientConnectionType)) {
        return provider;
      }
    }

    throw new IllegalArgumentException("No connection provider available for requested client connection type: " + clientConnectionType);
  }
}
