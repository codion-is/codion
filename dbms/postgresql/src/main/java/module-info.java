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
 * {@link is.codion.common.db.database.DatabaseFactory} implementation for PostgreSQL.
 * <p>
 * {@link is.codion.dbms.postgresql.PostgreSQLDatabaseFactory}
 * @provides is.codion.common.db.database.DatabaseFactory
 */
module is.codion.dbms.postgresql {
	requires transitive is.codion.common.db;

	exports is.codion.dbms.postgresql;

	provides is.codion.common.db.database.DatabaseFactory
					with is.codion.dbms.postgresql.PostgreSQLDatabaseFactory;
}