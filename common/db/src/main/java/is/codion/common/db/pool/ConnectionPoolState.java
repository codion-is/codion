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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.pool;

/**
 * An interface encapsulating the state of a connection pool at a given time.
 */
public interface ConnectionPoolState {

	/**
	 * @return the total number of connections being managed by the pool
	 */
	int size();

	/**
	 * @return the number of connections currently in use
	 */
	int inUse();

	/**
	 * @return the number of pending requests
	 */
	int waiting();

	/**
	 * @return the timestamp associated with this pool state
	 */
	long timestamp();
}
