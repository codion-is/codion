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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A default ConnectionPoolStatistics implementation
 */
final class DefaultConnectionPoolStatistics implements ConnectionPoolStatistics, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final String username;
	private long timestamp;
	private int connectionsInUse;
	private int availableInPool;

	private int connectionsCreated;
	private int connectionsDestroyed;
	private long creationDate;

	private List<ConnectionPoolState> snapshot = emptyList();
	private long resetDate;
	private int connectionRequests;
	private int requestsPerSecond;
	private int connectionRequestsFailed;
	private int requestsFailedPerSecond;
	private int averageTime = 0;
	private int minimumTime = 0;
	private int maximumTime = 0;

	/**
	 * @param username the database user the pool is based on
	 */
	DefaultConnectionPoolStatistics(String username) {
		this.username = username;
	}

	@Override
	public String username() {
		return username;
	}

	@Override
	public List<ConnectionPoolState> snapshot() {
		return snapshot;
	}

	@Override
	public int available() {
		return availableInPool;
	}

	@Override
	public int inUse() {
		return connectionsInUse;
	}

	@Override
	public long timestamp() {
		return timestamp;
	}

	@Override
	public long creationDate() {
		return this.creationDate;
	}

	@Override
	public int created() {
		return connectionsCreated;
	}

	@Override
	public int destroyed() {
		return connectionsDestroyed;
	}

	@Override
	public int requests() {
		return connectionRequests;
	}

	@Override
	public int failedRequests() {
		return connectionRequestsFailed;
	}

	@Override
	public int failedRequestsPerSecond() {
		return requestsFailedPerSecond;
	}

	@Override
	public int requestsPerSecond() {
		return requestsPerSecond;
	}

	@Override
	public long averageTime() {
		return averageTime;
	}

	@Override
	public long minimumTime() {
		return minimumTime;
	}

	@Override
	public long maximumTime() {
		return maximumTime;
	}

	@Override
	public int size() {
		return connectionsInUse + availableInPool;
	}

	@Override
	public long resetTime() {
		return resetDate;
	}

	void snapshot(List<ConnectionPoolState> snapshot) {
		this.snapshot = snapshot;
	}

	void availableInPool(int availableInPool) {
		this.availableInPool = availableInPool;
	}

	void connectionsInUse(int connectionsInUse) {
		this.connectionsInUse = connectionsInUse;
	}

	void timestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	void creationDate(long time) {
		this.creationDate = time;
	}

	void connectionsCreated(int connectionsCreated) {
		this.connectionsCreated = connectionsCreated;
	}

	void connectionsDestroyed(int connectionsDestroyed) {
		this.connectionsDestroyed = connectionsDestroyed;
	}

	void connectionRequests(int connectionRequests) {
		this.connectionRequests = connectionRequests;
	}

	void requestsPerSecond(int requestsPerSecond) {
		this.requestsPerSecond = requestsPerSecond;
	}

	void averageTime(int averageTime) {
		this.averageTime = averageTime;
	}

	void minimumTime(int minimumTime) {
		this.minimumTime = minimumTime;
	}

	void maximumTime(int maximumTime) {
		this.maximumTime = maximumTime;
	}

	void resetDate(long resetDate) {
		this.resetDate = resetDate;
	}

	void connectionRequestsFailed(int connectionRequestsFailed) {
		this.connectionRequestsFailed = connectionRequestsFailed;
	}

	void requestsFailedPerSecond(int requestsFailedPerSecond) {
		this.requestsFailedPerSecond = requestsFailedPerSecond;
	}
}
