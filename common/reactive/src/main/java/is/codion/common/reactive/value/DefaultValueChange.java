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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

final class DefaultValueChange<T> implements ValueChange<T> {

	private final @Nullable T previous;
	private final @Nullable T current;

	DefaultValueChange(@Nullable T previous, @Nullable T current) {
		this.previous = previous;
		this.current = current;
	}

	@Override
	public @Nullable T previous() {
		return previous;
	}

	@Override
	public @Nullable T current() {
		return current;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof ValueChange)) {
			return false;
		}
		ValueChange<?> that = (ValueChange<?>) object;

		return Objects.deepEquals(previous, that.previous()) && Objects.deepEquals(current, that.current());
	}

	@Override
	public int hashCode() {
		return Objects.hash(previous, current);
	}
}
