/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Configuration;
import org.jminor.common.EventListener;
import org.jminor.common.PropertyValue;
import org.jminor.common.User;
import org.jminor.common.Version;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Domain;

import java.util.UUID;

/**
 * Specifies a class resposible for providing a single {@link EntityConnection} instance.
 * {@link #getConnection()} is guaranteed to return a healthy connection or throw an exception.
 * @param <T> the type of {@link EntityConnection} provided
 */
public interface EntityConnectionProvider<T extends EntityConnection> {

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
  PropertyValue<String> CLIENT_DOMAIN_CLASS = Configuration.stringValue("jminor.client.domainClass", null);

  /**
   * Specifies whether the client should connect locally, remotely or via http,
   * accepted values: local, remote, http<br>
   * Value type: String<br>
   * Default value: local
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   * @see #CONNECTION_TYPE_HTTP
   */
  PropertyValue<String> CLIENT_CONNECTION_TYPE = Configuration.stringValue("jminor.client.connectionType", CONNECTION_TYPE_LOCAL);

  /**
   * Returns the domain model this connection is based on
   * @return the underlying domain model
   */
  Domain getDomain();

  /**
   * @return a EntityConditions instance based on the underlying domain model
   */
  EntityConditions getConditions();

  /**
   * Provides a EntityConnection object, is responsible for returning a healthy EntityConnection object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return a EntityConnection instance
   */
  T getConnection();

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
   * @return the name of the host providing the connection
   */
  String getServerHostName();

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
   * @param listener a listener notified each time the underlying connection is connected
   */
  void addOnConnectListener(final EventListener listener);

  /**
   * Removes the given listener
   * @param listener the listener to remove
   */
  void removeOnConnectListener(final EventListener listener);

  /**
   * Logs out, disconnects and performs cleanup if required
   */
  void disconnect();

  /**
   * Disconnects the underlying connection if connected.
   * @param user the user
   * @return this EntityConnectionProvider instance
   */
  EntityConnectionProvider setUser(final User user);

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
  EntityConnectionProvider setDomainClassName(final String domainClassName);

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
  EntityConnectionProvider setClientId(final UUID clientId);

  /**
   * @return the UUID identifying this client connection
   */
  UUID getClientId();

  /**
   * Disconnects the underlying connection if connected.
   * @param clientTypeId a String identifying the client type for this connection provider
   * @return this EntityConnectionProvider instance
   */
  EntityConnectionProvider setClientTypeId(final String clientTypeId);

  /**
   * @return the String identifying the client type for this connection provider
   */
  String getClientTypeId();

  /**
   * @param clientVersion the client version
   * @return this EntityConnectionProvider instance
   */
  EntityConnectionProvider setClientVersion(final Version clientVersion);

  /**
   * @return the client version
   */
  Version getClientVersion();
}
