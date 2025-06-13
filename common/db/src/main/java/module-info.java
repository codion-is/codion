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
 * <p>Core classes concerned with JDBC connectivity.
 * <ul>
 * <li>{@link is.codion.common.db.database.Database}
 * <li>{@link is.codion.common.db.database.DatabaseFactory}
 * <li>{@link is.codion.common.db.connection.DatabaseConnection}
 * <li>{@link is.codion.common.db.exception.DatabaseException}
 * <li>{@link is.codion.common.db.operation.DatabaseFunction}
 * <li>{@link is.codion.common.db.operation.DatabaseProcedure}
 * </ul>
 * @uses is.codion.common.db.database.DatabaseFactory
 * @uses is.codion.common.db.pool.ConnectionPoolFactory
 */
@org.jspecify.annotations.NullMarked
module is.codion.common.db {
	requires transitive org.jspecify;
	requires transitive java.sql;
	requires org.slf4j;
	requires transitive is.codion.common.core;

	exports is.codion.common.db.connection;
	exports is.codion.common.db.database;
	exports is.codion.common.db.exception;
	exports is.codion.common.db.operation;
	exports is.codion.common.db.pool;
	exports is.codion.common.db.report;
	exports is.codion.common.db.result;

	uses is.codion.common.db.database.DatabaseFactory;
	uses is.codion.common.db.pool.ConnectionPoolFactory;
}