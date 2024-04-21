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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An observable wrapper for a Set of values, including a possible null value.
 * A factory for {@link ValueSet} instances.
 * @param <T> the value type
 */
public interface ValueSet<T> extends Values<T, Set<T>> {

	@Override
	ValueSetObserver<T> observer();

	/**
	 * Creates a new empty {@link ValueSet}, using {@link Notify#WHEN_CHANGED}.
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet() {
		return builder(Collections.<T>emptySet()).build();
	}

	/**
	 * Creates a new {@link ValueSet}, using {@link Notify#WHEN_CHANGED}.
	 * @param initialValue the initial value, may not be null
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet(Collection<T> initialValue) {
		return builder(initialValue).build();
	}

	/**
	 * Creates a new {@link ValueSet.Builder} instance.
	 * @return a new builder
	 * @param <T> the value set type
	 */
	static <T> Builder<T> builder() {
		return builder(Collections.<T>emptySet());
	}

	/**
	 * Creates a new {@link ValueSet.Builder} instance.
	 * @param initialValue the initial value
	 * @return a new builder
	 * @param <T> the value set type
	 * @throws NullPointerException in case {@code initialValue} is null
	 */
	static <T> Builder<T> builder(Collection<T> initialValue) {
		return new DefaultValueSet.DefaultBuilder<T>()
						.initialValue(new LinkedHashSet<>(initialValue));
	}

	/**
	 * Builds a {@link ValueSet} instance.
	 * @param <T> the value type
	 */
	interface Builder<T> extends Values.Builder<T, Set<T>, Builder<T>> {

		/**
		 * @return a new {@link ValueSet} instance based on this builder
		 */
		ValueSet<T> build();
	}
}
