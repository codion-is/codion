/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Configuration;
import org.jminor.common.StateObserver;
import org.jminor.common.User;
import org.jminor.common.Value;
import org.jminor.common.Version;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;

import java.util.UUID;

/**
 * Interface for a class responsible for providing EntityConnection objects.
 */
public interface EntityConnectionProvider {

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
  Value<String> CLIENT_DOMAIN_CLASS = Configuration.stringValue("jminor.client.domainClass", null);

  /**
   * Specifies whether the client should connect locally, remotely or via http,
   * accepted values: local, remote, http<br>
   * Value type: String<br>
   * Default value: local
   * @see #CONNECTION_TYPE_LOCAL
   * @see #CONNECTION_TYPE_REMOTE
   * @see #CONNECTION_TYPE_HTTP
   */
  Value<String> CLIENT_CONNECTION_TYPE = Configuration.stringValue("jminor.client.connectionType", CONNECTION_TYPE_LOCAL);

  /**
   * Specifies whether client connections, remote or local, should schedule a periodic validity check of the connection.
   * Value type: Boolean<br>
   * Default value: true
   */
  Value<Boolean> CONNECTION_SCHEDULE_VALIDATION = Configuration.booleanValue("jminor.connection.scheduleValidation", true);

  /**
   * @return the underlying domain entities
   */
  Entities getDomain();

  /**
   * @return a EntityConditions instance based on the domain entities
   */
  EntityConditions getConditions();

  /**
   * Provides a EntityConnection object, is responsible for returning a healthy EntityConnection object,
   * that is, it must reconnect an invalid connection whether remotely or locally
   * @return a EntityConnection instance
   */
  EntityConnection getConnection();

  /**
   * Returns the Type this connection provider represents
   * @return the Type
   */
  EntityConnection.Type getConnectionType();

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
   * connection is healthy, only that one has been established.
   */
  boolean isConnected();

  /**
   * Returns a state which is active when this provider is connected, note that this state is only updated
   * if {@link EntityConnectionProvider#CONNECTION_SCHEDULE_VALIDATION} is set to true
   * @return a state active when this provider is connected
   */
  StateObserver getConnectedObserver();

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
