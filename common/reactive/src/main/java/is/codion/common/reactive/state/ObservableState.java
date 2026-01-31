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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observer;

/**
 * An observable boolean state, extending {@link Observer} to provide listener capabilities.
 * <p>
 * Listener methods are inherited from {@link Observer} and delegate to {@link #observer()}.
 * @see State
 */
public interface ObservableState extends Observer<Boolean> {

	/**
	 * @return the value of this state
	 */
	boolean is();

	/**
	 * @return A {@link ObservableState} instance that is always the reverse of this {@link ObservableState} instance
	 */
	ObservableState not();

	/**
	 * @return an {@link Observer} notified each time the observed value may have changed
	 */
	Observer<Boolean> observer();
}
