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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * Utility class for working with nulls.
 */
public final class Nulls {

	private Nulls() {}

	/**
	 * Included to skip the array creation in the varargs version
	 * @param object the object to check
	 * @return true if the object is non null
	 */
	public static boolean nonNull(@Nullable Object object) {
		return Objects.nonNull(object);
	}

	/**
	 * @param objects the objects to check
	 * @return true if none of the given objects are null
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
}
