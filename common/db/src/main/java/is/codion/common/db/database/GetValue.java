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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.db.database;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gets a single value from a {@link ResultSet}.
 * @param <T> the value type
 */
@FunctionalInterface
public interface GetValue<T> {

	/**
	 * Fetches a single value from a {@link ResultSet}.
	 * @param resultSet the {@link ResultSet}
	 * @param index the index of the column to fetch
	 * @return a single value fetched from the given {@link ResultSet}
	 * @throws SQLException in case of an exception
	 */
	@Nullable T get(ResultSet resultSet, int index) throws SQLException;
}
