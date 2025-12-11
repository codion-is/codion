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
package is.codion.common.reactive.observer;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Provides conditional listeners for an {@link Observer}.
 * {@snippet :
 * selection.item()
 *     .when(Objects::nonNull)
 *     .accept(this::onSelection);
 *
 * state.when(true)
 *     .run(this::onEnabled);
 *}
 * @param <T> the observed value type
 * @see Observer#when(Object)
 * @see Observer#when(Predicate)
 */
public interface Conditional<T> {

	/**
	 * Adds a {@link Runnable} to run when the condition is met.
	 * @param runnable the runnable to run
	 * @return the {@link Observer} for further configuration
	 */
	Observer<T> run(Runnable runnable);

	/**
	 * Adds a {@link Consumer} to call when the condition is met.
	 * @param consumer the consumer to call with the observed value
	 * @return the {@link Observer} for further configuration
	 */
	Observer<T> accept(Consumer<? super T> consumer);
}
