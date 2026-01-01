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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
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

	private long timestamp;
	private int inUse;
	private int available;
	private int created;
	private int destroyed;

	private List<ConnectionPoolState> snapshot = emptyList();
	private long resetTime;
	private int requests;
	private int requestsPerSecond;
	private int failedRequests;
	private int failedRequestsPerSecond;
	private int averageTime = 0;
	private int minimumTime = 0;
	private int maximumTime = 0;

	DefaultConnectionPoolStatistics() {}

	@Override
	public List<ConnectionPoolState> snapshot() {
		return snapshot;
	}

	@Override
	public int available() {
		return available;
	}

	@Override
	public int inUse() {
		return inUse;
	}

	@Override
	public long timestamp() {
		return timestamp;
	}

	@Override
	public int created() {
		return created;
	}

	@Override
	public int destroyed() {
		return destroyed;
	}

	@Override
	public int requests() {
		return requests;
	}

	@Override
	public int failedRequests() {
		return failedRequests;
	}

	@Override
	public int failedRequestsPerSecond() {
		return failedRequestsPerSecond;
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
		return inUse + available;
	}

	@Override
	public long resetTime() {
		return resetTime;
	}

	void snapshot(List<ConnectionPoolState> snapshot) {
		this.snapshot = snapshot;
	}

	void available(int available) {
		this.available = available;
	}

	void inUse(int inUse) {
		this.inUse = inUse;
	}

	void timestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	void created(int created) {
		this.created = created;
	}

	void destroyed(int destroyed) {
		this.destroyed = destroyed;
	}

	void requests(int requests) {
		this.requests = requests;
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

	void resetTime(long resetTime) {
		this.resetTime = resetTime;
	}

	void failedRequests(int failedRequests) {
		this.failedRequests = failedRequests;
	}

	void failedRequestsPerSecond(int failedRequestsPerSecond) {
		this.failedRequestsPerSecond = failedRequestsPerSecond;
	}
}
