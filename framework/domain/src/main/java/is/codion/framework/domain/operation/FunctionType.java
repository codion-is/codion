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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.operation;

import org.jspecify.annotations.Nullable;

/**
 * Identifies a database function.
 * @param <C> the connection type
 * <p>
 * The parameter and result types carry their nullness through, to the {@link DatabaseFunction} and the caller
 * executing it: a function declared with a non-null parameter receives one, and one declared with a non-null result
 * returns one, {@code @Nullable} marking either where null is allowed.
 * @param <P> the function parameter type, nullable where the function may be executed without one
 * @param <R> the function result type, nullable where the function may return null
 */
public interface FunctionType<C, P extends @Nullable Object, R extends @Nullable Object> {

	/**
	 * @return the function name
	 */
	String name();

	/**
	 * Creates a {@link FunctionType} with the given name.
	 * @param name the name
	 * @param <C> the connection type
	 * @param <P> the function parameter type
	 * @param <R> the function result type
	 * @return a new {@link FunctionType}
	 */
	static <C, P extends @Nullable Object, R extends @Nullable Object> FunctionType<C, P, R> functionType(String name) {
		return new DefaultFunctionType<>(name);
	}
}
