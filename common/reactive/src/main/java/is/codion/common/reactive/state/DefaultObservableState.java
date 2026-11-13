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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.AbstractObserver;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

final class DefaultObservableState extends AbstractObserver<Boolean> implements ObservableState {

	private final ObservableState state;
	private final boolean not;

	private @Nullable DefaultObservableState notObserver;

	DefaultObservableState(ObservableState state, boolean not) {
		this.state = requireNonNull(state);
		this.not = not;
	}

	@Override
	public String toString() {
		return Boolean.toString(is());
	}

	//not synchronized, reading final fields, since the state's set() takes the state's lock and then this one, to
	//notify, and a lock taken here before the state's would deadlock a reader on another thread against it
	@Override
	public boolean is() {
		return not ? !state.is() : state.is();
	}

	@Override
	public synchronized ObservableState not() {
		if (not) {
			return state;
		}
		if (notObserver == null) {
			notObserver = new DefaultObservableState(this, true);
		}

		return notObserver;
	}

	//notifies outside this lock, which a when() losing its last listener on another thread takes to detach
	//from this state, while holding its own, which notifying it takes
	void notifyObservers(boolean newValue, boolean previousValue) {
		if (previousValue != newValue) {
			notifyListeners(newValue);
			DefaultObservableState notState = notObserver();
			if (notState != null) {
				notState.notifyObservers(previousValue, newValue);
			}
		}
	}

	private synchronized @Nullable DefaultObservableState notObserver() {
		return notObserver;
	}
}
