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
package is.codion.common.value;

import java.util.Collection;

/**
 * A read only values observer
 * @param <T> the values type
 * @param <C> the collection type
 */
public interface ValuesObserver<T, C extends Collection<T>> extends ValueObserver<C>, Iterable<T> {

	/**
	 * Returns true if this {@link Values} instance contains the specified element
	 * @param value the element
	 * @return true if this {@link Values} instance contains the specified element
	 */
	boolean contains(T value);

	/**
	 * Returns true if this {@link Values} instance contains all of the elements of the specified collection
	 * @param values the elements to check
	 * @return true if this {@link Values} instance contains all of the elements of the specified collection
	 */
	boolean containsAll(Collection<T> values);

	/**
	 * @return true if this {@link Values} instance is empty
	 */
	boolean empty();

	/**
	 * @return true if this {@link Values} instance is not empty
	 */
	boolean notEmpty();

	/**
	 * @return the number of elements in this {@link Values} instance
	 */
	int size();
}
