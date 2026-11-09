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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observable;

import is.codion.common.reactive.observer.Observer;

/**
 * Observes the changes of an {@link Observable}, notified with a {@link Change} when the observable notifies a value
 * differing from the one before, by {@link java.util.Objects#deepEquals(Object, Object)}.
 * <p>{@link #previous()} and {@link #current()} observe the values themselves, for a method reference taking the value:
 * {@code observable.changed().current().addConsumer(this::load)}. On a change, the consumers of this observer are
 * notified first, then those of {@link #previous()}, then those of {@link #current()}.
 * @param <T> the value type
 * @see Observable#changed()
 */
public interface ChangeObserver<T> extends Observer<Change<T>> {

	/**
	 * @return an {@link Observer} notified with the {@link Change#previous()} value on each change
	 */
	Observer<T> previous();

	/**
	 * @return an {@link Observer} notified with the {@link Change#current()} value on each change
	 */
	Observer<T> current();
}
