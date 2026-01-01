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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.item;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

abstract class AbstractItem<T> implements Item<T>, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final @Nullable T value;

	/**
	 * Creates a new Item.
	 * @param value the value, may be null
	 */
	AbstractItem(@Nullable T value) {
		this.value = value;
	}

	@Override
	public final @Nullable T get() {
		return value;
	}

	@Override
	public final T getOrThrow() {
		if (value == null) {
			throw new IllegalStateException("Item is null");
		}

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
		return this == obj || obj instanceof Item && Objects.equals(value, ((Item<?>) obj).get());
	}

	@Override
	public final int hashCode() {
		return value == null ? 0 : value.hashCode();
	}
}
