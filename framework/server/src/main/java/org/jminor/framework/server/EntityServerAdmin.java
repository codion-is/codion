/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.pool.ConnectionPoolStatistics;
import org.jminor.common.rmi.server.ClientLog;
import org.jminor.common.rmi.server.RemoteClient;
import org.jminor.common.rmi.server.ServerInformation;
import org.jminor.common.user.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines the server admin service methods.
 */
public interface EntityServerAdmin extends Remote {

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
  ServerInformation getServerInfo() throws RemoteException;

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
  void setConnectionLimit(int value) throws RemoteException;

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
   * @param clientId the ID of the client for which to retrieve the log
   * @return the log for the given connection
   * @throws RemoteException in case of a communication error
   */
  ClientLog getClientLog(UUID clientId) throws RemoteException;

  /**
   * Returns true if logging is enabled for the given connection
   * @param clientId the ID of the client
   * @return true if logging is on for the given connection
   * @throws RemoteException in case of a communication error
   */
  boolean isLoggingEnabled(UUID clientId) throws RemoteException;

  /**
   * Sets the logging status for the given connection
   * @param clientId the ID of the client
   * @param status the new logging status
   * @throws RemoteException in case of a communication error
   */
  void setLoggingEnabled(UUID clientId, boolean status) throws RemoteException;

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
  void disconnect(UUID clientId) throws RemoteException;

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
  Collection<RemoteClient> getClients(User user) throws RemoteException;

  /**
   * @param clientTypeId the client type for which to retrieve the clients
   * @return the clients associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> getClients(String clientTypeId) throws RemoteException;

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
   * @return the identifiers of the client types connected to the server
   * @throws RemoteException in case of an exception
   */
  Collection<String> getClientTypes() throws RemoteException;

  /**
   * @return a list containing usernames backed by a connection pool
   * @throws RemoteException in case of an exception
   */
  List<String> getConnectionPools() throws RemoteException;

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
   * @return the number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getRequestsPerSecond() throws RemoteException;

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
   * @param value true if statistics should be collected for a snapshot of the given connection pool
   * @throws RemoteException in case of an exception
   */
  void setCollectPoolSnapshotStatistics(String username, boolean value) throws RemoteException;

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
  List<GcEvent> getGcEvents(long since) throws RemoteException;

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

  /**
   * @param since the time since from which to retrieve statistics
   * @return current statistics for this server
   * @throws RemoteException in case of an exception
   */
  ServerStatistics getServerStatistics(long since) throws RemoteException;

  /**
   * Basic server performance statistics.
   */
  interface ServerStatistics {

    /**
     * @return the timestamp
     */
    long getTimestamp();

    /**
     * @return the connection count
     */
    int getConnectionCount();

    /**
     * @return the connection limit
     */
    int getConnectionLimit();

    /**
     * @return used memory
     */
    long getUsedMemory();

    /**
     * @return maximum memory
     */
    long getMaximumMemory();

    /**
     * @return allocated memory
     */
    long getAllocatedMemory();

    /**
     * @return requests per second
     */
    int getRequestsPerSecond();

    /**
     * @return the system cpu load
     */
    double getSystemCpuLoad();

    /**
     * @return the process cpu load
     */
    double getProcessCpuLoad();

    /**
     * @return thread statistics
     */
    ThreadStatistics getThreadStatistics();

    /**
     * @return GC events
     */
    List<GcEvent> getGcEvents();
  }

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
    long getTimestamp();

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
