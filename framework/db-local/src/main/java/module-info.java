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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
/**
 * Local JDBC based database connection classes.
 * <ul>
 * <li>{@link is.codion.framework.db.local.LocalEntityConnection}
 * <li>{@link is.codion.framework.db.local.LocalEntityConnectionProvider}
 * </ul>
 * @provides is.codion.framework.db.EntityConnectionProvider
 */
@org.jspecify.annotations.NullMarked
module is.codion.framework.db.local {
	requires org.slf4j;
	requires transitive is.codion.framework.db.core;

	exports is.codion.framework.db.local;
	exports is.codion.framework.db.local.logger
					to is.codion.framework.server;

	provides is.codion.framework.db.EntityConnectionProvider.Builder
					with is.codion.framework.db.local.DefaultLocalEntityConnectionProviderBuilder;
	provides is.codion.framework.db.EntityQueries.Factory
					with is.codion.framework.db.local.DefaultEntityQueriesFactory;
}