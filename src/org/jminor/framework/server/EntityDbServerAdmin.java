/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.User;
import org.jminor.common.db.pool.ConnectionPoolSettings;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;
import org.jminor.framework.domain.EntityDefinition;

import org.apache.log4j.Level;

import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Defines the server admin service methods.
 * User: darri
 * Date: 19.2.2007
 * Time: 09:41:35
 */
public interface EntityDbServerAdmin extends Remote {

  /**
   * Shuts down the server
   * @throws RemoteException in case of a communication error
   */
  void shutdown() throws RemoteException;

  /**
   * @return the server startup date
   * @throws RemoteException in case of a communication error
   */
  Date getStartDate() throws RemoteException;

  /**
   * @return the database URL
   * @throws RemoteException in case of a communication error
   */
  String getDatabaseURL() throws RemoteException;

  /**
   * @return the server name
   * @throws RemoteException in case of a communication error
   */
  String getServerName() throws RemoteException;

  /**
   * @return the server port
   * @throws RemoteException in case of a communication error
   */
  int getServerPort() throws RemoteException;

  /**
   * @return the port on which db connections are created
   * @throws RemoteException in case of a communication error
   */
  int getServerDbPort() throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  int getConnectionCount() throws RemoteException;

  /**
   * @return the number of seconds that should pass between maintenance cycles,
   * that is, when inactive clients are purged
   * @throws RemoteException in case of a communication error
   */
  int getCheckMaintenanceInterval() throws RemoteException;

  /**
   * @param interval the number of seconds that should pass between maintenance cycles,
   * that is, when inactive clients are purged
   * @throws RemoteException in case of a communication error
   */
  void setCheckMaintenanceInterval(final int interval) throws RemoteException;

  /**
   * @param connectionKey the key of the connection for which to retrieve the log
   * @return the log for the given connection
   * @throws RemoteException in case of a communication error
   */
  ServerLog getServerLog(final String connectionKey) throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  int getActiveConnectionCount() throws RemoteException;

  /**
   * Returns true if logging is enabled for the given connection
   * @param connectionKey the key of the clint
   * @return true if logging is on for the given connection
   * @throws RemoteException in case of a communication error
   */
  boolean isLoggingOn(final String connectionKey) throws RemoteException;

  /**
   * Sets the logging status for the given connection
   * @param connectionKey the key of the connection
   * @param status the new logging status
   * @throws RemoteException in case of a communication error
   */
  void setLoggingOn(final String connectionKey, final boolean status) throws RemoteException;

  /**
   * @return a string containing memory usage information
   * @throws RemoteException in case of a communication error
   */
  String getMemoryUsage() throws RemoteException;

  /**
   * Performs garbage collection
   * @throws RemoteException in case of a communication error
   */
  void performGC() throws RemoteException;

  /**
   * Unregisters the connection from the server, if connection pooling is enabled
   * for the user the connection is pooled.
   * @param connectionKey the key of the connection
   * @throws RemoteException in case of a communication error
   */
  void disconnect(final String connectionKey) throws RemoteException;

  /**
   * @return the server logging level
   * @throws RemoteException in case of a communication error
   */
  Level getLoggingLevel() throws RemoteException;

  /**
   * @param level the logging level
   * @throws RemoteException in case of a communication error
   */
  void setLoggingLevel(final Level level) throws RemoteException;

  /**
   * @return the users currently connected to the server
   * @throws RemoteException in case of a communication error
   */
  Collection<User> getUsers() throws RemoteException;

  /**
   * @param user the user for which to retrieve the connection keys
   * @return the connection keys associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<ClientInfo> getClients(final User user) throws RemoteException;

  /**
   * Returns the connection timeout in seconds
   * @return the connection timeout in seconds
   * @throws RemoteException in case of a communication error
   */
  int getConnectionTimeout() throws RemoteException;

  /**
   * Sets the connection timeout in seconds
   * @param timeout the timeout in seconds
   * @throws RemoteException in case of a communication error
   */
  void setConnectionTimeout(final int timeout) throws RemoteException;

  Collection<String> getClientTypes() throws RemoteException;

  List<ConnectionPoolSettings> getEnabledConnectionPools() throws RemoteException;

  void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException;

  ConnectionPoolSettings getConnectionPoolSettings(final User user) throws RemoteException;

  ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException;

  DatabaseStatistics getDatabaseStatistics() throws RemoteException;

  int getRequestsPerSecond() throws RemoteException;

  int getWarningTimeThreshold() throws RemoteException;

  void setWarningTimeThreshold(final int threshold) throws RemoteException;

  int getWarningTimeExceededPerSecond() throws RemoteException;

  void removeConnections(final boolean inactiveOnly) throws RemoteException;

  void resetConnectionPoolStatistics(final User user) throws RemoteException;

  boolean isCollectFineGrainedPoolStatistics(final User user) throws RemoteException;

  void setCollectFineGrainedPoolStatistics(final User user, final boolean value) throws RemoteException;

  String getSystemProperties() throws RemoteException;

  void loadDomainModel(final URL location, final String domainClassName) throws RemoteException, ClassNotFoundException,
          IllegalAccessException, InstantiationException;

  Map<String, EntityDefinition> getEntityDefinitions() throws RemoteException;
}
