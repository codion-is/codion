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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.user.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administration interface for a {@link java.rmi.server.RemoteServer}.
 */
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
	 * @param clientType the type of clients to retrieve
	 * @return the clients of the given type
	 * @throws RemoteException in case of a communication error
	 */
	Collection<RemoteClient> clients(String clientType) throws RemoteException;

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
	long totalMemory() throws RemoteException;

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
		 * @return used memory in bytes
		 */
		long usedMemory();

		/**
		 * @return maximum memory in bytes
		 */
		long maximumMemory();

		/**
		 * @return total memory in bytes
		 */
		long totalMemory();

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
		 * @return a map containing the number of threads mapped to each thread state
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
