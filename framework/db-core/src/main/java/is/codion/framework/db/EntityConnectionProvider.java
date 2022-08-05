/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
 * {@link #connection()} is guaranteed to return a healthy connection or throw an exception.
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
  PropertyValue<String> CLIENT_DOMAIN_CLASS = Configuration.stringValue("codion.client.domainClass");

  /**
   * Specifies whether the client should connect locally, via rmi or http,
   * accepted values: local, remote, http<br>
   * Value type: String<br>
   * Default value: local
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   * @see #CONNECTION_TYPE_HTTP
   */
  PropertyValue<String> CLIENT_CONNECTION_TYPE = Configuration.stringValue("codion.client.connectionType", CONNECTION_TYPE_LOCAL);

  /**
   * Returns the domain entities this connection is based on
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * Provides a EntityConnection object, is responsible for returning a healthy EntityConnection object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return a EntityConnection instance
   */
  EntityConnection connection();

  /**
   * Returns a String specifying the type of connection provided by this connection provider
   * @return a String specifying the type of connection, e.g. "local" or "remote"
   */
  String connectionType();

  /**
   * @return a short description of the database provider
   */
  String description();

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
   * @return the user used by this connection provider
   */
  User user();

  /**
   * @return the domain model classname
   */
  String domainClassName();

  /**
   * @return the UUID identifying this client connection
   */
  UUID clientId();

  /**
   * @return the String identifying the client type for this connection provider
   */
  String clientTypeId();

  /**
   * @return the client version
   */
  Version clientVersion();

  /**
   * @return a unconfigured {@link Builder} instance,
   * based on {@link EntityConnectionProvider#CLIENT_CONNECTION_TYPE} configuration value
   * @see EntityConnectionProvider#CLIENT_CONNECTION_TYPE
   */
  static Builder<?, ?> builder() {
    String clientConnectionType = CLIENT_CONNECTION_TYPE.getOrThrow();
    for (Builder<?, ?> builder : ServiceLoader.load(Builder.class)) {
      if (builder.connectionType().equalsIgnoreCase(clientConnectionType)) {
        return builder;
      }
    }

    throw new IllegalArgumentException("No connection provider builder available for requested client connection type: " + clientConnectionType);
  }

  /**
   * Builds a {@link EntityConnectionProvider} instances
   * @param <T> the connection provider type
   * @param <B> the builder type
   */
  interface Builder<T extends EntityConnectionProvider, B extends Builder<T, B>> {

    /**
     * Returns a String specifying the type of connection provided by this connection provider builder
     * @return a String specifying the type of connection, e.g. "local" or "remote"
     */
    String connectionType();

    /**
     * @param user the user
     * @return this builder instance
     */
    B user(User user);

    /**
     * @param domainClassName the name of the class specifying the domain model for this connection provider
     * @return this builder instance
     */
    B domainClassName(String domainClassName);

    /**
     * @param clientId the UUID identifying this client connection
     * @return this builder instance
     */
    B clientId(UUID clientId);

    /**
     * @param clientTypeId a String identifying the client type for this connection provider
     * @return this builder instance
     */
    B clientTypeId(String clientTypeId);

    /**
     * @param clientVersion the client version
     * @return this builder instance
     */
    B clientVersion(Version clientVersion);

    /**
     * Builds a {@link EntityConnectionProvider} instance based on this builder
     * @return a new {@link EntityConnectionProvider} instance
     */
    T build();
  }
}
