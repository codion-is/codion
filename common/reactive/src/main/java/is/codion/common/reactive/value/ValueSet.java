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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <p>An observable wrapper for a Set of values.
 * <p>A factory for {@link ValueSet} instances.
 * <p>All implementations are thread-safe and support concurrent access.
 * @param <T> the value type
 */
public interface ValueSet<T> extends ValueCollection<T, Set<T>> {

	@Override
	ObservableValueSet<T> observable();

	/**
	 * Creates a new empty {@link ValueSet}, using {@link Notify#CHANGED}.
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 */
	static <T> ValueSet<T> valueSet() {
		return builder(Collections.<T>emptySet()).build();
	}

	/**
	 * Creates a new {@link ValueSet}, using {@link Notify#CHANGED}.
	 * @param values the initial values, may not be null
	 * @param <T> the value type
	 * @return a new {@link ValueSet}
	 * @throws NullPointerException in case {@code values} is null
	 */
	static <T> ValueSet<T> valueSet(Collection<T> values) {
		return builder(values).build();
	}

	/**
	 * Creates a new {@link ValueSet.Builder} instance.
	 * @param <T> the value set type
	 * @return a new builder
	 */
	static <T> Builder<T> builder() {
		return builder(Collections.<T>emptySet());
	}

	/**
	 * Creates a new {@link ValueSet.Builder} instance.
	 * @param values the initial values, may not be null
	 * @param <T> the value set type
	 * @return a new builder
	 * @throws NullPointerException in case {@code values} is null
	 */
	static <T> Builder<T> builder(Collection<T> values) {
		requireNonNull(values);

		return new DefaultValueSet.DefaultBuilder<T>()
						.value(new LinkedHashSet<>(values));
	}

	/**
	 * Builds a {@link ValueSet} instance.
	 * @param <T> the value type
	 */
	interface Builder<T> extends ValueCollection.Builder<T, Set<T>, Builder<T>> {

		/**
		 * @return a new {@link ValueSet} instance based on this builder
		 */
		ValueSet<T> build();
	}
}
