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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for working with nulls.
 */
public final class Nulls {

	private Nulls() {}

	/**
	 * Returns true if none of the given objects are null, false if the array itself is null,
	 * and true if no objects are provided.
	 * <p>Use {@link Objects#nonNull(Object)} for single values, to eliminate varargs array creation.
	 * @param objects the objects to check
	 * @return true if none of the given objects are null, false if the array itself is null,
	 * true if no objects are provided
	 */
	public static boolean nonNull(@Nullable Object... objects) {
		if (objects == null) {
			return false;
		}
		if (objects.length == 0) {
			return true;
		}
		return Arrays.stream(objects).noneMatch(Objects::isNull);
	}

	/**
	 * Throws a NullPointerException if the given collection, or any of its items, is null
	 * @param items the items to check for nulls
	 * @return the items
	 * @param <C> the collection type
	 * @param <T> the collection element type
	 */
	public static <C extends Collection<T>, T> C rejectNulls(@Nullable C items) {
		for (T item : requireNonNull(items)) {
			requireNonNull(item);
		}

		return items;
	}
}
