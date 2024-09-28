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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.item;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

abstract class AbstractItem<T> implements Item<T>, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final T value;

	/**
	 * Creates a new Item.
	 * @param value the value, may be null
	 */
	AbstractItem(T value) {
		this.value = value;
	}

	@Override
	public final T value() {
		return value;
	}

	/**
	 * @return the caption
	 */
	@Override
	public final String toString() {
		return caption();
	}

	@Override
	public final boolean equals(Object obj) {
		return this == obj || obj instanceof Item && Objects.equals(value, ((Item<?>) obj).value());
	}

	@Override
	public final int hashCode() {
		return value == null ? 0 : value.hashCode();
	}
}
