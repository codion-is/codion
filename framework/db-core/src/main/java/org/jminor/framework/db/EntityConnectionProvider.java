/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.Configuration;
import org.jminor.common.StateObserver;
import org.jminor.common.User;
import org.jminor.common.Value;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;

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
   * Specifies the class providing http db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.plugins.db.http.HttpEntityConnectionProvider
   */
  Value<String> HTTP_CONNECTION_PROVIDER = Configuration.stringValue("jminor.client.httpConnectionProvider", "org.jminor.framework.db.http.HttpEntityConnectionProvider");

  /**
   * Specifies the class providing remote db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.db.remote.RemoteEntityConnectionProvider
   */
  Value<String> REMOTE_CONNECTION_PROVIDER = Configuration.stringValue("jminor.client.remoteConnectionProvider", "org.jminor.framework.db.remote.RemoteEntityConnectionProvider");

  /**
   * Specifies the class providing local db connections<br>
   * Value type: String (the name of a class implementing org.jminor.framework.db.EntityConnectionProvider)<br>
   * Default value: org.jminor.framework.db.local.LocalEntityConnectionProvider
   */
  Value<String> LOCAL_CONNECTION_PROVIDER = Configuration.stringValue("jminor.client.localConnectionProvider", "org.jminor.framework.db.local.LocalEntityConnectionProvider");

  /**
   * Specifies whether client connections, remote or local, should schedule a periodic validity check of the connection.
   * Value type: Boolean<br>
   * Default value: true
   */
  Value<Boolean> CONNECTION_SCHEDULE_VALIDATION = Configuration.booleanValue("jminor.connection.scheduleValidation", true);

  /**
   * @return the underlying domain entities
   */
  Entities getEntities();

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
   * Sets the user for this connection provider, this invalidates and disconnects the previous connection if any.
   * @param user the user
   */
  void setUser(final User user);

  /**
   * @return the user used by this connection provider
   */
  User getUser();
}
