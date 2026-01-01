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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.operation;

import org.jspecify.annotations.Nullable;

/**
 * A database function
 * @param <C> the connection type required by this function
 * @param <T> the parameter type
 * @param <R> the return type
 */
public interface DatabaseFunction<C, T, R> {

	/**
	 * Executes this function using the given connection
	 * @param connection the connection to use
	 * @param parameter the function parameter, if any
	 * @return the function return parameter
	 */
	@Nullable R execute(C connection, @Nullable T parameter);
}
