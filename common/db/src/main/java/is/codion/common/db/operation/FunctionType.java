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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.operation;

/**
 * @param <C> the connection type
 * @param <T> the function parameter type
 * @param <R> the function result type
 */
public interface FunctionType<C, T, R> {

	/**
	 * @return the function name
	 */
	String name();

	/**
	 * Creates a {@link FunctionType} with the given name and types.
	 * @param name the name
	 * @param <C> the connection type
	 * @param <T> the function parameter type
	 * @param <R> the function result type
	 * @return a new {@link FunctionType}
	 */
	static <C, T, R> FunctionType<C, T, R> functionType(String name) {
		return new DefaultFunctionType<>(name);
	}
}
