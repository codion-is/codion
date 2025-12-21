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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.jspecify.annotations.Nullable;

/**
 * Represents a value change
 * @param <T> the value type
 */
public interface ValueChange<T> {

	/**
	 * @return the previous value
	 */
	@Nullable T previous();

	/**
	 * @return the current value
	 */
	@Nullable T current();

	/**
	 * Instantiates a new {@link ValueChange} instance
	 * @param previous the previous value
	 * @param current the current value
	 * @param <T> the value type
	 * @return a new {@link ValueChange} instance
	 */
	static <T> ValueChange<T> valueChange(@Nullable T previous, @Nullable T current) {
		return new DefaultValueChange<>(previous, current);
	}
}
