/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  Collection<User> users() throws RemoteException;

  /**
   * @return all clients connected to the server
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> clients() throws RemoteException;

  /**
   * @param user the user for which to retrieve the clients
   * @return the clients associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> clients(User user) throws RemoteException;

  /**
   * @param clientTypeId the client type for which to retrieve the clients
   * @return the clients associated with the given user
   * @throws RemoteException in case of a communication error
   */
  Collection<RemoteClient> clients(String clientTypeId) throws RemoteException;

  /**
   * @return the identifiers of the client types connected to the server
   * @throws RemoteException in case of an exception
   */
  Collection<String> clientTypes() throws RemoteException;

  /**
   * Shuts down the server
   * @throws RemoteException in case of a communication error
   */
  void shutdown() throws RemoteException;

  /**
   * @return static information about the server
   * @throws RemoteException in case of an exception
   */
  ServerInformation serverInformation() throws RemoteException;

  /**
   * @return the number of service requests per second
   * @throws RemoteException in case of an exception
   */
  int requestsPerSecond() throws RemoteException;

  /**
   * @return the number of active connections
   * @throws RemoteException in case of a communication error
   */
  int connectionCount() throws RemoteException;

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
  long allocatedMemory() throws RemoteException;

  /**
   * @return the amount of memory being used by the server process
   * @throws RemoteException in case of a communication error
   */
  long usedMemory() throws RemoteException;

  /**
   * @return the maximum amount of memory available to the server process
   * @throws RemoteException in case of a communication error
   */
  long maxMemory() throws RemoteException;

  /**
   * @return the system cpu load, a negative number if not available
   * @throws RemoteException in case of a communication error
   * @see com.sun.management.OperatingSystemMXBean#getSystemCpuLoad()
   */
  double systemCpuLoad() throws RemoteException;

  /**
   * @return the java vm process cpu load, a negative number if not available
   * @throws RemoteException in case of a communication error
   * @see com.sun.management.OperatingSystemMXBean#getProcessCpuLoad()
   */
  double processCpuLoad() throws RemoteException;

  /**
   * @return the server system properties
   * @throws RemoteException in case of an exception
   */
  String systemProperties() throws RemoteException;

  /**
   * @param since the time since from which to get gc events
   * @return a list containing garbage collection notifications
   * @throws RemoteException in case of an exception
   */
  List<GcEvent> gcEvents(long since) throws RemoteException;

  /**
   * @return current thread statistics
   * @throws RemoteException in case of an exception
   */
  ThreadStatistics threadStatistics() throws RemoteException;

  /**
   * @param since the time since from which to retrieve statistics
   * @return current statistics for this server
   * @throws RemoteException in case of an exception
   */
  ServerStatistics serverStatistics(long since) throws RemoteException;

  /**
   * Basic server performance statistics.
   */
  interface ServerStatistics {

    /**
     * @return the timestamp
     */
    long timestamp();

    /**
     * @return the connection count
     */
    int connectionCount();

    /**
     * @return the connection limit
     */
    int connectionLimit();

    /**
     * @return used memory
     */
    long usedMemory();

    /**
     * @return maximum memory
     */
    long maximumMemory();

    /**
     * @return allocated memory
     */
    long allocatedMemory();

    /**
     * @return requests per second
     */
    int requestsPerSecond();

    /**
     * @return the system cpu load
     */
    double systemCpuLoad();

    /**
     * @return the process cpu load
     */
    double processCpuLoad();

    /**
     * @return thread statistics
     */
    ThreadStatistics threadStatistics();

    /**
     * @return GC events
     */
    List<GcEvent> gcEvents();
  }

  /**
   * Thread statistics
   */
  interface ThreadStatistics {

    /**
     * @return the number of threads
     */
    int threadCount();

    /**
     * @return the number daemon threads
     */
    int daemonThreadCount();

    /**
     * @return the number of threads in each state
     */
    Map<Thread.State, Integer> threadStateCount();
  }

  /**
   * Garbage collection event
   */
  interface GcEvent {

    /**
     * @return event time stamp
     */
    long timestamp();

    /**
     * @return event gc name
     */
    String gcName();

    /**
     * @return event duration
     */
    long duration();
  }
}
