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
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.logging.MethodTrace;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;

import java.util.List;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.integerValue;

/**
 * A class responsible for managing a local EntityConnection.
 * @see LocalEntityConnectionProvider#builder()
 */
public interface LocalEntityConnectionProvider extends EntityConnectionProvider {

	/**
	 * Specifies whether method tracing is enabled by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> TRACING = booleanValue("codion.db.local.tracing", false);

	/**
	 * Specifies the default maximum number of method traces to keep for a local connection.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 50
	 * </ul>
	 */
	PropertyValue<Integer> TRACES = integerValue("codion.db.local.traces", 50);

	/**
	 * @return the underlying domain model
	 */
	Domain domain();

	/**
	 * @return the underlying {@link Database} instance
	 */
	Database database();

	@Override
	LocalEntityConnection connection();

	/**
	 * @return the {@link State} controlling whether method tracing is enabled
	 */
	State tracing();

	/**
	 * @return the collected method traces
	 */
	List<MethodTrace> traces();

	/**
	 * Instantiates a new builder instance.
	 * @return a new builder
	 */
	static Builder builder() {
		return new DefaultLocalEntityConnectionProviderBuilder();
	}

	/**
	 * Builds a {@link LocalEntityConnectionProvider}.
	 */
	interface Builder extends EntityConnectionProvider.Builder<LocalEntityConnectionProvider, Builder> {

		/**
		 * @param database the database instance to use
		 * @return this builder instance
		 */
		Builder database(Database database);

		/**
		 * @param domain the domain model to base this connection on
		 * @return this builder instance
		 */
		Builder domain(Domain domain);

		/**
		 * @param queryTimeout the default query timeout
		 * @return this builder instance
		 */
		Builder queryTimeout(int queryTimeout);
	}
}