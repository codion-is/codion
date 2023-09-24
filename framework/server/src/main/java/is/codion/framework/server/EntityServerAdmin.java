/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
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
  String databaseUrl() throws RemoteException;

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
  ClientLog clientLog(UUID clientId) throws RemoteException;

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
   * Returns the idle connection timeout in ms
   * @return the idle connection timeout in ms
   * @throws RemoteException in case of a communication error
   */
  int getIdleConnectionTimeout() throws RemoteException;

  /**
   * Sets the idle connection timeout in ms
   * @param idleConnectionTimeout the timeout in ms
   * @throws RemoteException in case of a communication error
   * @throws IllegalArgumentException in case timeout is less than zero
   */
  void setIdleConnectionTimeout(int idleConnectionTimeout) throws RemoteException;

  /**
   * @return a collection containing usernames backed by a connection pool
   * @throws RemoteException in case of an exception
   */
  Collection<String> connectionPoolUsernames() throws RemoteException;

  /**
   * @param username the username
   * @param since the time since from which to retrieve pool statistics
   * @return the pool statistics
   * @throws RemoteException in case of an exception
   */
  ConnectionPoolStatistics connectionPoolStatistics(String username, long since) throws RemoteException;

  /**
   * Returns the statistics gathered via {@link Database#queryCounter()}.
   * @return a {@link Database.Statistics} object containing query statistics collected since
   * the last time this function was called.
   * @throws RemoteException in case of an exception
   */
  Database.Statistics databaseStatistics() throws RemoteException;

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
   * @param username the username
   * @return true if check out times statistics should be collected for the given connection pool
   * @throws RemoteException in case of an exception
   */
  boolean isCollectPoolCheckOutTimes(String username) throws RemoteException;

  /**
   * @param username the username
   * @param collectCheckOutTimes true if check out times should be collected for the given connection pool
   * @throws RemoteException in case of an exception
   */
  void setCollectPoolCheckOutTimes(String username, boolean collectCheckOutTimes) throws RemoteException;

  /**
   * @return a map containing each domain name and its entity definitions
   * @throws RemoteException in case of an exception
   */
  Map<String, Collection<DomainEntityDefinition>> domainEntityDefinitions() throws RemoteException;

  /**
   * @return a map containing each domain name with its reports
   * @throws RemoteException in case of an exception
   */
  Map<String, Collection<DomainReport>> domainReports() throws RemoteException;

  /**
   * @return a map containing each domain name with its operations
   * @throws RemoteException in case of an exception
   */
  Map<String, Collection<DomainOperation>> domainOperations() throws RemoteException;

  /**
   * Clears any cached reports
   * @throws RemoteException in case of an exception
   */
  void clearReportCache() throws RemoteException;

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
  int getPooledConnectionIdleTimeout(String username) throws RemoteException;

  /**
   * @param username the username
   * @param pooledConnectionIdleTimeout the pooled connection timeout in ms
   * @throws RemoteException in case of an exception
   */
  void setPooledConnectionIdleTimeout(String username, int pooledConnectionIdleTimeout) throws RemoteException;

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
   * Basic information about a entity definition.
   */
  interface DomainEntityDefinition {

    /**
     * @return the entity name
     */
    String name();

    /**
     * @return the table name
     */
    String tableName();
  }

  /**
   * Basic information about a report.
   */
  interface DomainReport {

    /**
     * @return the report name
     */
    String name();

    /**
     * @return the report type
     */
    String type();

    /**
     * @return the report path
     */
    String path();

    /**
     * @return true if the report has been cached
     */
    boolean isCached();
  }

  /**
   * Basic information about an operation.
   */
  interface DomainOperation {

    /**
     * @return the operation type
     */
    String type();

    /**
     * @return the operation name
     */
    String name();

    /**
     * @return the operation class name
     */
    String className();
  }
}
