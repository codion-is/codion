/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.DatabaseStatistics;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.model.User;
import org.jminor.common.server.ClientInfo;
import org.jminor.common.server.ServerLog;

import org.apache.log4j.Level;

import java.net.URI;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines the server admin service methods.
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
  long getStartDate() throws RemoteException;

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
  void setMaintenanceInterval(final int interval) throws RemoteException;

  /**
   * @param clientID the ID of the client for which to retrieve the log
   * @return the log for the given connection
   * @throws RemoteException in case of a communication error
   */
  ServerLog getServerLog(final UUID clientID) throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  int getActiveConnectionCount() throws RemoteException;

  /**
   * Returns true if logging is enabled for the given connection
   * @param clientID the ID of the client
   * @return true if logging is on for the given connection
   * @throws RemoteException in case of a communication error
   */
  boolean isLoggingOn(final UUID clientID) throws RemoteException;

  /**
   * Sets the logging status for the given connection
   * @param clientID the ID of the client
   * @param status the new logging status
   * @throws RemoteException in case of a communication error
   */
  void setLoggingOn(final UUID clientID, final boolean status) throws RemoteException;

  /**
   * @return a string containing memory usage information
   * @throws RemoteException in case of a communication error
   */
  String getMemoryUsage() throws RemoteException;

  /**
   * @return the total amount of memory allocated by the server process
   * @throws RemoteException in case of a communication error
   */
  long getAllocatedMemory() throws RemoteException;

  /**
   * @return the amount of memory being used by the server process
   * @throws RemoteException in case of a communication error
   */
  long getUsedMemory() throws RemoteException;

  /**
   * @return the maximum amount of memory available to the server process
   * @throws RemoteException in case of a communication error
   */
  long getMaxMemory() throws RemoteException;

  /**
   * Performs garbage collection
   * @throws RemoteException in case of a communication error
   */
  void performGC() throws RemoteException;

  /**
   * Unregisters the connection from the server, if connection pooling is enabled
   * for the user the connection is pooled.
   * @param clientID the ID of the client
   * @throws RemoteException in case of a communication error
   */
  void disconnect(final UUID clientID) throws RemoteException;

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
   * @return the info on all clients connected to the server
   * @throws RemoteException in case of a communication error
   */
  Collection<ClientInfo> getClients() throws RemoteException;

  /**
   * @param user the user for which to retrieve the client infos
   * @return the connection keys associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<ClientInfo> getClients(final User user) throws RemoteException;

  /**
   * @param clientTypeID the client type for which to retrieve the client infos
   * @return the connection keys associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<ClientInfo> getClients(final String clientTypeID) throws RemoteException;

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
   */
  void setConnectionTimeout(final int timeout) throws RemoteException;

  /**
   * @return the identifiers of the client types connected to the server
   * @throws RemoteException in case of an exception
   */
  Collection<String> getClientTypes() throws RemoteException;

  /**
   * @return a list of the usernames behind the active connection pools
   * @throws RemoteException in case of an exception
   */
  List<User> getEnabledConnectionPools() throws RemoteException;

  /**
   * @param user the pool user
   * @param since the time since from which to retrieve pool statistics
   * @return the pool statistics
   * @throws RemoteException in case of an exception
   */
  ConnectionPoolStatistics getConnectionPoolStatistics(final User user, final long since) throws RemoteException;

  /**
   * @return usage statistics for the underlying database
   * @throws RemoteException in case of an exception
   */
  DatabaseStatistics getDatabaseStatistics() throws RemoteException;

  /**
   * @return the number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getRequestsPerSecond() throws RemoteException;

  /**
   *
   * @return the request warning time threshold in ms
   * @throws RemoteException in case of an exception
   */
  int getWarningTimeThreshold() throws RemoteException;

  /**
   * @param threshold the request warning time threshold in ms
   * @throws RemoteException in case of an exception
   */
  void setWarningTimeThreshold(final int threshold) throws RemoteException;

  /**
   * @return the number of service requests exceeding the warning time
   * @throws RemoteException in case of an exception
   * @see #setWarningTimeThreshold(int)
   */
  int getWarningTimeExceededPerSecond() throws RemoteException;

  /**
   * Removes connections.
   * @param inactiveOnly if true, only inactive connections are removed
   * @throws RemoteException in case of an exception
   */
  void removeConnections(final boolean inactiveOnly) throws RemoteException;

  /**
   * Resets the statistics that have been collected so far
   * @param user the pool user
   * @throws RemoteException in case of an exception
   */
  void resetConnectionPoolStatistics(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @return true if fine grained statistics should be collected for the given connection pool
   * @throws RemoteException in case of an exception
   */
  boolean isCollectFineGrainedPoolStatistics(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param value true if fine grained statistics should be collected for the given connection pool
   * @throws RemoteException in case of an exception
   */
  void setCollectFineGrainedPoolStatistics(final User user, final boolean value) throws RemoteException;

  /**
   * @return the server system properties
   * @throws RemoteException in case of an exception
   */
  String getSystemProperties() throws RemoteException;

  /**
   * Loads the domain model found int he given class, at the given location
   * @param location the classpath URI
   * @param domainClassName the domain class name
   * @throws RemoteException in case of an exception
   * @throws ClassNotFoundException in case the domain class in not found
   * @throws IllegalAccessException in case the class loader access is denied
   */
  void loadDomainModel(final URI location, final String domainClassName) throws RemoteException, ClassNotFoundException,
          IllegalAccessException;

  /**
   * @return a map containing all entityIDs, with their respective table names as an associated value
   * @throws RemoteException in case of an exception
   */
  Map<String,String> getEntityDefinitions() throws RemoteException;

  /**
   * @param user the pool user
   * @return the pool cleanup interval in ms
   * @throws RemoteException in case of an exception
   */
  int getConnectionPoolCleanupInterval(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param poolCleanupInterval the pool cleanup interval in ms
   * @throws RemoteException in case of an exception
   */
  void setConnectionPoolCleanupInterval(final User user, final int poolCleanupInterval) throws RemoteException;

  /**
   * @param user the pool user
   * @return the pool enabled status
   * @throws RemoteException in case of an exception
   */
  boolean isConnectionPoolEnabled(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param enabled the pool enabled status
   * @throws RemoteException in case of an exception
   */
  void setConnectionPoolEnabled(final User user, final boolean enabled) throws RemoteException;

  /**
   * @param user the pool user
   * @return the pooled connection timeout in ms
   * @throws RemoteException in case of an exception
   */
  int getPooledConnectionTimeout(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param timeout the pooled connection timeout in ms
   * @throws RemoteException in case of an exception
   */
  void setPooledConnectionTimeout(final User user, final int timeout) throws RemoteException;

  /**
   * @param user the pool user
   * @return the maximum time to wait between check out retries in ms
   * @throws RemoteException in case of an exception
   */
  int getMaximumPoolRetryWaitPeriod(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param value the maximum time to wait between check out retries in ms
   * @throws RemoteException in case of an exception
   */
  void setMaximumPoolRetryWaitPeriod(final User user, final int value) throws RemoteException;

  /**
   * @param user the pool user
   * @return the maximum time in ms to retry checking out a connection before throwing an exception
   * @throws RemoteException in case of an exception
   */
  int getMaximumPoolCheckOutTime(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param value the maximum time in ms to retry checking out a connection before throwing an exception
   * @throws RemoteException in case of an exception
   */
  void setMaximumPoolCheckOutTime(final User user, final int value) throws RemoteException;

  /**
   * @param user the pool user
   * @return the maximum connection pool size
   * @throws RemoteException in case of an exception
   */
  int getMaximumConnectionPoolSize(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param value the maximum connection pool size
   * @throws RemoteException in case of an exception
   */
  void setMaximumConnectionPoolSize(final User user, final int value) throws RemoteException;

  /**
   * @param user the pool user
   * @return the minimum connection pool size
   * @throws RemoteException in case of an exception
   */
  int getMinimumConnectionPoolSize(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param value the minimum connection pool size
   * @throws RemoteException in case of an exception
   */
  void setMinimumConnectionPoolSize(final User user, final int value) throws RemoteException;
}
