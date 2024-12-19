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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.result;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * A {@link ResultPacker} packs the contents of a {@link ResultSet} into a {@link List}.
 * @param <T> the type of object resulting from the packing
 */
public interface ResultPacker<T> {

	/**
	 * Iterates through the given {@link ResultSet}, packing its contents into a {@link List} using {@link #get(ResultSet)} in the order they appear.
	 * This method does not close the {@link ResultSet}.
	 * @param resultSet the {@link ResultSet} instance containing the query result to process
	 * @return a {@link List} containing the data from the query result
	 * @throws SQLException thrown if anything goes wrong during the packing
	 * @throws NullPointerException in case {@code resultSet} is null
	 */
	default List<T> pack(ResultSet resultSet) throws SQLException {
		requireNonNull(resultSet);
		List<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(get(resultSet));
		}

		return result;
	}

	/**
	 * Fetches a single instance from the given {@link ResultSet}, assuming {@link ResultSet#next()} has been called
	 * @param resultSet the {@link ResultSet}
	 * @return the instance fetched from the {@link ResultSet}
	 * @throws SQLException in case of failure
	 */
	T get(ResultSet resultSet) throws SQLException;
}