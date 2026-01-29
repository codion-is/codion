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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
/**
 * Core framework database connection classes, such as:
 * <ul>
 * <li>{@link is.codion.framework.db.EntityConnection}
 * <li>{@link is.codion.framework.db.EntityConnectionProvider}
 * </ul>
 * @uses is.codion.framework.db.EntityConnectionProvider.Builder
 */
@org.jspecify.annotations.NullMarked
module is.codion.framework.db.core {
	requires org.slf4j;
	requires transitive is.codion.framework.domain;

	exports is.codion.framework.db;
	exports is.codion.framework.db.exception;

	uses is.codion.framework.db.EntityConnectionProvider.Builder;
	uses is.codion.framework.db.EntityQueries.Factory;
}