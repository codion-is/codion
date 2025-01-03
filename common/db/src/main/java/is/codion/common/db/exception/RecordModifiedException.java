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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.db.exception;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * An exception indicating that the row in question has been modified or deleted since it was loaded.
 */
public final class RecordModifiedException extends UpdateException {

	private final Object row;
	private final @Nullable Object modifiedRow;

	/**
	 * Instantiates a new RecordModifiedException
	 * @param row the row being updated
	 * @param modifiedRow the current (modified) version of the row, null if it has been deleted
	 * @param message a message describing the modification
	 */
	public RecordModifiedException(Object row, @Nullable Object modifiedRow, @Nullable String message) {
		super(message);
		this.row = requireNonNull(row);
		this.modifiedRow = modifiedRow;
	}

	/**
	 * @return the row being updated
	 */
	public Object row() {
		return row;
	}

	/**
	 * @return the current (modified) version of the row, null if it has been deleted
	 */
	public @Nullable Object modifiedRow() {
		return modifiedRow;
	}
}
