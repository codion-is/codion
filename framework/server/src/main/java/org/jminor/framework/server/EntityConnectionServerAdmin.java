/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.remote.ClientLog;
import org.jminor.common.remote.RemoteClient;
import org.jminor.common.remote.Server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines the server admin service methods.
 */
public interface EntityConnectionServerAdmin extends Remote {

  /**
   * Shuts down the server
   * @throws RemoteException in case of a communication error
   */
  void shutdown() throws RemoteException;

  /**
   * @return the database URL
   * @throws RemoteException in case of a communication error
   */
  String getDatabaseURL() throws RemoteException;

  /**
   * @return static information about the server
   * @throws RemoteException in case of an exception
   */
  Server.ServerInfo getServerInfo() throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  int getConnectionCount() throws RemoteException;

  /**
   * @return the maximum number of concurrent connections this servers accepts
   * @throws RemoteException in case of a communication error
   */
  int getConnectionLimit() throws RemoteException;

  /**
   * @param value the maximum number of concurrent connections this servers accepts
   * @throws RemoteException in case of a communication error
   */
  void setConnectionLimit(final int value) throws RemoteException;

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
   * @param clientId the ID of the client for which to retrieve the log
   * @return the log for the given connection
   * @throws RemoteException in case of a communication error
   */
  ClientLog getClientLog(final UUID clientId) throws RemoteException;

  /**
   * Returns true if logging is enabled for the given connection
   * @param clientId the ID of the client
   * @return true if logging is on for the given connection
   * @throws RemoteException in case of a communication error
   */
  boolean isLoggingEnabled(final UUID clientId) throws RemoteException;

  /**
   * Sets the logging status for the given connection
   * @param clientId the ID of the client
   * @param status the new logging status
   * @throws RemoteException in case of a communication error
   */
  void setLoggingEnabled(final UUID clientId, final boolean status) throws RemoteException;

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
   * @return the system cpu load, a negative number if not available
   * @throws RemoteException in case of a communication error
   * @see com.sun.management.OperatingSystemMXBean#getSystemCpuLoad()
   */
  double getSystemCpuLoad() throws RemoteException;

  /**
   * @return the java vm process cpu load, a negative number if not available
   * @throws RemoteException in case of a communication error
   * @see com.sun.management.OperatingSystemMXBean#getProcessCpuLoad()
   */
  double getProcessCpuLoad() throws RemoteException;

  /**
   * Unregisters the connection from the server, if connection pooling is enabled
   * for the user the connection is pooled.
   * @param clientId the ID of the client
   * @throws RemoteException in case of a communication error
   */
  void disconnect(final UUID clientId) throws RemoteException;

  /**
   * @return the server logging level
   * @throws RemoteException in case of a communication error
   */
  Object getLoggingLevel() throws RemoteException;

  /**
   * @param level the logging level
   * @throws RemoteException in case of a communication error
   */
  void setLoggingLevel(final Object level) throws RemoteException;

  /**
   * @return the users currently connected to the server
   * @throws RemoteException in case of a communication error
   */
  Collection<User> getUsers() throws RemoteException;

  /**
   * @return all clients connected to the server
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> getClients() throws RemoteException;

  /**
   * @param user the user for which to retrieve the clients
   * @return the clients associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> getClients(final User user) throws RemoteException;

  /**
   * @param clientTypeId the client type for which to retrieve the clients
   * @return the clients associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> getClients(final String clientTypeId) throws RemoteException;

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
  void setConnectionTimeout(final int timeout) throws RemoteException;

  /**
   * @return the identifiers of the client types connected to the server
   * @throws RemoteException in case of an exception
   */
  Collection<String> getClientTypes() throws RemoteException;

  /**
   * @return a list containing users backed by a connection pool
   * @throws RemoteException in case of an exception
   */
  List<User> getConnectionPools() throws RemoteException;

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
  Database.Statistics getDatabaseStatistics() throws RemoteException;

  /**
   * @return the number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getRequestsPerSecond() throws RemoteException;

  /**
   * Removes connections.
   * @param timedOutOnly if true only connections that have timed out are culled
   * @throws RemoteException in case of an exception
   */
  void disconnectClients(final boolean timedOutOnly) throws RemoteException;

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
   * @param since the time since from which to get gc events
   * @return a list containing garbage collection notifications
   * @throws RemoteException in case of an exception
   */
  List<GcEvent> getGcEvents(final long since) throws RemoteException;

  /**
   * @return current thread statistics
   * @throws RemoteException in case of an exception
   */
  ThreadStatistics getThreadStatistics() throws RemoteException;

  /**
   * @return a map containing all entityIds, with their respective table names as an associated value
   * @throws RemoteException in case of an exception
   */
  Map<String, String> getEntityDefinitions() throws RemoteException;

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

  /**
   * @param user the pool user
   * @return the number of milliseconds to wait before trying to create a new connection
   * @throws RemoteException in case of an exception
   */
  int getPoolConnectionThreshold(final User user) throws RemoteException;

  /**
   * @param user the pool user
   * @param value the number of milliseconds to wait before trying to create a new connection
   * @throws RemoteException in case of an exception
   */
  void setPoolConnectionThreshold(final User user, final int value) throws RemoteException;

  /**
   * Thread statistics
   */
  interface ThreadStatistics {

    /**
     * @return the number of threads
     */
    int getThreadCount();

    /**
     * @return the number daemon threads
     */
    int getDaemonThreadCount();

    /**
     * @return the number of threads in each state
     */
    Map<Thread.State, Integer> getThreadStateCount();
  }

  /**
   * Garbage collection event
   */
  interface GcEvent {

    /**
     * @return event time stamp
     */
    long getTimeStamp();

    /**
     * @return event gc name
     */
    String getGcName();

    /**
     * @return event duration
     */
    long getDuration();
  }
}
