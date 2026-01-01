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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * <p>An observable wrapper for a List of values.
 * <p>A factory for {@link ValueList} instances.
 * <p>All implementations are thread-safe and support concurrent access.
 * @param <T> the value type
 */
public interface ValueList<T> extends ValueCollection<T, List<T>> {

	@Override
	ObservableValueList<T> observable();

	/**
	 * Creates a new empty {@link ValueList}, using {@link Notify#CHANGED}.
	 * @param <T> the value type
	 * @return a new {@link ValueList}
	 */
	static <T> ValueList<T> valueList() {
		return builder(Collections.<T>emptyList()).build();
	}

	/**
	 * Creates a new {@link ValueList}, using {@link Notify#CHANGED}.
	 * @param values the initial values, may not be null
	 * @param <T> the value type
	 * @return a new {@link ValueList}
	 * @throws NullPointerException in case {@code values} is null
	 */
	static <T> ValueList<T> valueList(Collection<T> values) {
		return builder(values).build();
	}

	/**
	 * Creates a new {@link ValueList.Builder} instance.
	 * @param <T> the value set type
	 * @return a new builder
	 */
	static <T> Builder<T> builder() {
		return builder(Collections.<T>emptyList());
	}

	/**
	 * Creates a new {@link ValueList.Builder} instance.
	 * @param values the initial values, may not be null
	 * @param <T> the value set type
	 * @return a new builder
	 * @throws NullPointerException in case {@code values} is null
	 */
	static <T> Builder<T> builder(Collection<T> values) {
		requireNonNull(values);

		return new DefaultValueList.DefaultBuilder<T>()
						.value(new ArrayList<>(values));
	}

	/**
	 * Builds a {@link ValueList} instance.
	 * @param <T> the value type
	 */
	interface Builder<T> extends ValueCollection.Builder<T, List<T>, Builder<T>> {

		/**
		 * @return a new {@link ValueList} instance based on this builder
		 */
		ValueList<T> build();
	}
}
