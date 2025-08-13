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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.AbstractEntityConnectionProvider.AbstractBuilder;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Builds a {@link LocalEntityConnectionProvider} instance.
 * @see LocalEntityConnectionProvider#builder()
 */
public final class DefaultLocalEntityConnectionProviderBuilder
				extends AbstractBuilder<LocalEntityConnectionProvider, LocalEntityConnectionProvider.Builder>
				implements LocalEntityConnectionProvider.Builder {

	@Nullable Domain domain;
	@Nullable Database database;
	int queryTimeout = LocalEntityConnection.QUERY_TIMEOUT.getOrThrow();

	/**
	 * Instantiates a new {@link DefaultLocalEntityConnectionProviderBuilder}
	 */
	public DefaultLocalEntityConnectionProviderBuilder() {
		super(EntityConnectionProvider.CONNECTION_TYPE_LOCAL);
	}

	@Override
	public LocalEntityConnectionProvider.Builder database(Database database) {
		this.database = requireNonNull(database);
		return this;
	}

	@Override
	public LocalEntityConnectionProvider.Builder domain(Domain domain) {
		this.domain = requireNonNull(domain);
		return domain(domain.type());
	}

	@Override
	public LocalEntityConnectionProvider.Builder queryTimeout(int queryTimeout) {
		if (queryTimeout < 0) {
			throw new IllegalArgumentException("queryTimeout must be >= 0");
		}
		this.queryTimeout = queryTimeout;
		return this;
	}

	@Override
	public LocalEntityConnectionProvider build() {
		return new DefaultLocalEntityConnectionProvider(this);
	}
}
