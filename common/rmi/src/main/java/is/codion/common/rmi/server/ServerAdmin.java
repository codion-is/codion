/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.rmi.server;

import is.codion.common.user.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ServerAdmin extends Remote {

  /**
   * Unregisters the connection from the server, if connection pooling is enabled
   * for the user the connection is pooled.
   * @param clientId the id of the client
   * @throws RemoteException in case of a communication error
   */
  void disconnect(UUID clientId) throws RemoteException;

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
   * @return the identifiers of the client types connected to the server
   * @throws RemoteException in case of an exception
   */
  Collection<String> getClientTypes() throws RemoteException;

  /**
   * Shuts down the server
   * @throws RemoteException in case of a communication error
   */
  void shutdown() throws RemoteException;

  /**
   * @return static information about the server
   * @throws RemoteException in case of an exception
   */
  ServerInformation getServerInformation() throws RemoteException;

  /**
   * @return the number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int getRequestsPerSecond() throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  int getConnectionCount() throws RemoteException;

  /**
   * @return the maximum number of concurrent connections this server accepts
   * @throws RemoteException in case of a communication error
   */
  int getConnectionLimit() throws RemoteException;

  /**
   * @param value the maximum number of concurrent connections this server accepts
   * @throws RemoteException in case of a communication error
   */
  void setConnectionLimit(int value) throws RemoteException;

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
