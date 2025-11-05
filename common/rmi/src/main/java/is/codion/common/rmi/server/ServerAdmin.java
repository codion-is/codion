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

import is.codion.common.utilities.user.User;

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
	 * Disconnects the given client from the server
	 * @param clientId the id of the client to disconnect
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
	 * @return the server system properties
	 * @throws RemoteException in case of an exception
	 */
	String systemProperties() throws RemoteException;

	/**
	 * @return the active serialization filter patterns
	 * @throws RemoteException in case of an exception
	 */
	String serializationFilterPatterns() throws RemoteException;

	/**
	 * @param since the time since from which to retrieve statistics
	 * @return current statistics for this server
	 * @throws RemoteException in case of an exception
	 */
	ServerStatistics statistics(long since) throws RemoteException;

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
