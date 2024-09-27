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
package is.codion.common.model.selection;

import is.codion.common.model.CancelException;
import is.codion.common.observer.Mutable;
import is.codion.common.observer.Observer;
import is.codion.common.state.StateObserver;

/**
 * A selection model, managing a single selected item
 * @param <R> the item type
 */
public interface SingleSelection<R> {

	/**
	 * @return a {@link StateObserver} indicating whether the selection is empty
	 */
	StateObserver empty();

	/**
	 * To prevent a selection change, add a listener throwing a {@link CancelException}.
	 * @return an observer notified when the selection is about to change
	 */
	Observer<?> changing();

	/**
	 * @return a {@link Mutable} controlling the selected item
	 */
	Mutable<R> item();

	/**
	 * Clears the selection
	 */
	void clear();
}
