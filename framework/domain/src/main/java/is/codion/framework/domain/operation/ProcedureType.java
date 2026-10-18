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
package is.codion.framework.domain.operation;

import org.jspecify.annotations.Nullable;

/**
 * Identifies a database procedure.
 * @param <C> the connection type
 * <p>
 * The parameter type carries its nullness through to the {@link DatabaseProcedure}: a procedure declared with a non-null
 * parameter receives one, {@code @Nullable} marking it where null is allowed.
 * @param <P> the procedure parameter type, nullable where the procedure may be executed without one
 */
public interface ProcedureType<C, P extends @Nullable Object> {

	/**
	 * @return the procedure name
	 */
	String name();

	/**
	 * Creates a {@link ProcedureType} with the given name.
	 * @param name the name
	 * @param <C> the connection type
	 * @param <P> the procedure parameter type
	 * @return a new {@link ProcedureType}
	 */
	static <C, P extends @Nullable Object> ProcedureType<C, P> procedureType(String name) {
		return new DefaultProcedureType<>(name);
	}
}
