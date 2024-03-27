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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

import java.io.Serializable;

/**
 * A default ConnectionPoolState implementation
 */
final class DefaultConnectionPoolState implements ConnectionPoolState, Serializable {

	private static final long serialVersionUID = 1;

	private long timestamp;
	private int size = -1;
	private int inUse = -1;
	private int waiting = -1;

	DefaultConnectionPoolState set(long timestamp, int size, int inUse, int waiting) {
		this.timestamp = timestamp;
		this.size = size;
		this.inUse = inUse;
		this.waiting = waiting;

		return this;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int inUse() {
		return inUse;
	}

	@Override
	public int waiting() {
		return waiting;
	}

	@Override
	public long timestamp() {
		return timestamp;
	}
}
