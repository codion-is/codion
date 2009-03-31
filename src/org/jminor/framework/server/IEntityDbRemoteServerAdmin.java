/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.db.ConnectionPoolSettings;
import org.jminor.common.db.DbLog;
import org.jminor.common.db.User;
import org.jminor.common.remote.RemoteClient;

import org.apache.log4j.Level;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Defines the server admin service methods
 * User: darri
 * Date: 19.2.2007
 * Time: 09:41:35
 */
public interface IEntityDbRemoteServerAdmin extends Remote {

  public void shutdown() throws RemoteException;

  /**
   * @return the server startup date
   * @throws RemoteException in case of a communication error
   */
  public Date getStartDate() throws RemoteException;

  /**
   * @return the database URL
   * @throws RemoteException in case of a communication error
   */
  public String getDatabaseURL() throws RemoteException;

  /**
   * @return the server name
   * @throws RemoteException in case of a communication error
   */
  public String getServerName() throws RemoteException;

  /**
   * @return the server port
   * @throws RemoteException in case of a communication error
   */
  public int getServerPort() throws RemoteException;

  /**
   * @return the port on which db connections are created
   * @throws RemoteException in case of a communication error
   */
  public int getServerDbPort() throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  public int getConnectionCount() throws RemoteException;

  /**
   * @return the number of seconds that should pass between maintenance cycles,
   * that is, when inactive clients are purged
   * @throws RemoteException in case of a communication error
   */
  public int getCheckMaintenanceInterval() throws RemoteException;

  /**
   * @param interval the number of seconds that should pass between maintenance cycles,
   * that is, when inactive clients are purged
   * @throws RemoteException in case of a communication error
   */
  public void setCheckMaintenanceInterval(final int interval) throws RemoteException;

  /**
   * @param connectionKey the key of the connection for which to retrieve the log @return the log for the given connection
   * @return the log for the given connection
   * @throws RemoteException in case of a communication error
   */
  public DbLog getConnectionLog(final String connectionKey) throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  public int getActiveConnectionCount() throws RemoteException;

  /**
   * Returns true if logging is enabled for the given connection
   * @param connectionKey the key of the clint @return true if logging is enabled
   * @return true if logging is on for the given connection
   * @throws RemoteException in case of a communication error
   */
  public boolean isLoggingOn(final String connectionKey) throws RemoteException;

  /**
   * Sets the logging status for the given connection
   * @param connectionKey the key of the connection
   * @param status the new logging status @throws RemoteException in case of a communication error
   * @throws RemoteException in case of a communication error
   */
  public void setLoggingOn(final String connectionKey, final boolean status) throws RemoteException;

  /**
   * @return a string containing memory usage information
   * @throws RemoteException in case of a communication error
   */
  public String getMemoryUsage() throws RemoteException;

  /**
   * Performs garbage collection
   * @throws RemoteException in case of a communication error
   */
  public void performGC() throws RemoteException;

  /**
   * Unregisters the connection from the server, if connection pooling is enabled
   * for the user the connection is pooled.
   * @param connectionKey the key of the connection
   * @throws RemoteException in case of a communication error
   */
  public void disconnect(final String connectionKey) throws RemoteException;

  /**
   * @return the server logging level
   * @throws RemoteException in case of a communication error
   */
  public Level getLoggingLevel() throws RemoteException;

  /**
   * @param level the logging level
   * @throws RemoteException in case of a communication error
   */
  public void setLoggingLevel(final Level level) throws RemoteException;

  /**
   * @return the users currently connected to the server
   * @throws RemoteException in case of a communication error
   */
  public Collection<User> getUsers() throws RemoteException;

  /**
   * @param user the user for which to retrieve the connection keys
   * @return the connection keys associated with the given user
   * @throws RemoteException in case of a communication error
   */
  public Collection<RemoteClient> getClients(final User user) throws RemoteException;

  public Collection<String> getClientTypes() throws RemoteException;

  public List<ConnectionPoolSettings> getActiveConnectionPools() throws RemoteException;

  public void setConnectionPoolSettings(final ConnectionPoolSettings settings) throws RemoteException;

  public ConnectionPoolSettings getConnectionPoolSettings(final User user, final long since) throws RemoteException;

  public int getRequestsPerSecond() throws RemoteException;

  public int getWarningTimeThreshold() throws RemoteException;

  public void setWarningTimeThreshold(final int threshold) throws RemoteException;

  public int getWarningTimeExceededPerSecond() throws RemoteException;

  public void removeConnections(final boolean inactiveOnly) throws RemoteException;

  public void resetConnectionPoolStatistics(final User user) throws RemoteException;
}
