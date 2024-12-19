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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.db.result;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

final class DefaultIterator<T> implements Iterator<T> {

	private final ResultIterator<T> resultIterator;

	DefaultIterator(ResultIterator<T> resultIterator) {
		this.resultIterator = requireNonNull(resultIterator);
	}

	@Override
	public boolean hasNext() {
		try {
			return resultIterator.hasNext();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public T next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		try {
			return resultIterator.next();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
