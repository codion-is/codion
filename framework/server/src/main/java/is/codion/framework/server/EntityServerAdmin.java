/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.pool.ConnectionPoolStatistics;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.rmi.server.ServerAdmin;

import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Defines the entity server admin service methods.
 */
public interface EntityServerAdmin extends ServerAdmin {

  /**
   * @return the database URL
   * @throws RemoteException in case of a communication error
   */
  String getDatabaseUrl() throws RemoteException;

  /**
   * @return the number of ms that should pass between maintenance cycles,
   * that is, when inactive clients are purged
   * @throws RemoteException in case of a communication error
   */
  int getMaintenanceInterval() throws RemoteException;

  /**
   * @param interval the number of ms that should pass between maintenance cycles,
   * that is, when inactive clients are purged
   * @throws RemoteException in case of a communication error
   */
  void setMaintenanceInterval(int interval) throws RemoteException;

  /**
   * @param clientId the id of the client for which to retrieve the log
   * @return the log for the given connection
   * @throws RemoteException in case of a communication error
   */
  ClientLog getClientLog(UUID clientId) throws RemoteException;

  /**
   * Returns true if logging is enabled for the given connection
   * @param clientId the id of the client
   * @return true if logging is on for the given connection
   * @throws RemoteException in case of a communication error
   */
  boolean isLoggingEnabled(UUID clientId) throws RemoteException;

  /**
   * Sets the logging status for the given connection
   * @param clientId the id of the client
   * @param loggingEnabled the new logging status
   * @throws RemoteException in case of a communication error
   */
  void setLoggingEnabled(UUID clientId, boolean loggingEnabled) throws RemoteException;

  /**
   * @return the server log level
   * @throws RemoteException in case of a communication error
   */
  Object getLogLevel() throws RemoteException;

  /**
   * @param level the log level
   * @throws RemoteException in case of a communication error
   */
  void setLogLevel(Object level) throws RemoteException;

  /**
   * Returns the connection timeout in ms
   * @return the connection timeout in ms
   * @throws RemoteException in case of a communication error
   */
  int getConnectionTimeout() throws RemoteException;

  /**
   * Sets the connection timeout in ms
   * @param timeout the timeout in ms
   * @throws RemoteException in case of a communication error
   * @throws IllegalArgumentException in case timeout is less than zero
   */
  void setConnectionTimeout(int timeout) throws RemoteException;

  /**
   * @return a collection containing usernames backed by a connection pool
   * @throws RemoteException in case of an exception
   */
  Collection<String> getConnectionPoolUsernames() throws RemoteException;

  /**
   * @param username the username
   * @param since the time since from which to retrieve pool statistics
   * @return the pool statistics
   * @throws RemoteException in case of an exception
   */
  ConnectionPoolStatistics getConnectionPoolStatistics(String username, long since) throws RemoteException;

  /**
   * Returns the statistics gathered via {@link Database#countQuery(String)}.
   * @return a {@link Database.Statistics} object containing query statistics collected since
   * the last time this function was called.
   * @throws RemoteException in case of an exception
   */
  Database.Statistics getDatabaseStatistics() throws RemoteException;

  /**
   * Disconnects all timed-out clients.
   * @throws RemoteException in case of an exception
   */
  void disconnectTimedOutClients() throws RemoteException;

  /**
   * Disconnects all connected clients.
   * @throws RemoteException in case of an exception
   */
  void disconnectAllClients() throws RemoteException;

  /**
   * Resets the statistics that have been collected so far
   * @param username the username
   * @throws RemoteException in case of an exception
   */
  void resetConnectionPoolStatistics(String username) throws RemoteException;

  /**
   * @param username the username
   * @return true if snapshot statistics should be collected for the given connection pool
   * @throws RemoteException in case of an exception
   */
  boolean isCollectPoolSnapshotStatistics(String username) throws RemoteException;

  /**
   * @param username the username
   * @param snapshotStatistics true if statistics should be collected for a snapshot of the given connection pool
   * @throws RemoteException in case of an exception
   */
  void setCollectPoolSnapshotStatistics(String username, boolean snapshotStatistics) throws RemoteException;

  /**
   * @return a map containing all entityType names, with their respective table names as an associated value
   * @throws RemoteException in case of an exception
   */
  Map<String, String> getEntityDefinitions() throws RemoteException;

  /**
   * @param username the username
   * @return the pool cleanup interval in ms
   * @throws RemoteException in case of an exception
   */
  int getConnectionPoolCleanupInterval(String username) throws RemoteException;

  /**
   * @param username the username
   * @param poolCleanupInterval the pool cleanup interval in ms
   * @throws RemoteException in case of an exception
   */
  void setConnectionPoolCleanupInterval(String username, int poolCleanupInterval) throws RemoteException;

  /**
   * @param username the username
   * @return the pooled connection timeout in ms
   * @throws RemoteException in case of an exception
   */
  int getPooledConnectionTimeout(String username) throws RemoteException;

  /**
   * @param username the username
   * @param timeout the pooled connection timeout in ms
   * @throws RemoteException in case of an exception
   */
  void setPooledConnectionTimeout(String username, int timeout) throws RemoteException;

  /**
   * @param username the username
   * @return the maximum time in ms to retry checking out a connection before throwing an exception
   * @throws RemoteException in case of an exception
   */
  int getMaximumPoolCheckOutTime(String username) throws RemoteException;

  /**
   * @param username the username
   * @param value the maximum time in ms to retry checking out a connection before throwing an exception
   * @throws RemoteException in case of an exception
   */
  void setMaximumPoolCheckOutTime(String username, int value) throws RemoteException;

  /**
   * @param username the username
   * @return the maximum connection pool size
   * @throws RemoteException in case of an exception
   */
  int getMaximumConnectionPoolSize(String username) throws RemoteException;

  /**
   * @param username the username
   * @param value the maximum connection pool size
   * @throws RemoteException in case of an exception
   */
  void setMaximumConnectionPoolSize(String username, int value) throws RemoteException;

  /**
   * @param username the username
   * @return the minimum connection pool size
   * @throws RemoteException in case of an exception
   */
  int getMinimumConnectionPoolSize(String username) throws RemoteException;

  /**
   * @param username the username
   * @param value the minimum connection pool size
   * @throws RemoteException in case of an exception
   */
  void setMinimumConnectionPoolSize(String username, int value) throws RemoteException;
}
