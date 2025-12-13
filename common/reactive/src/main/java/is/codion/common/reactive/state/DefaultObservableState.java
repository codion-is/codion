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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.DefaultObserver;
import is.codion.common.reactive.observer.Observer;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

final class DefaultObservableState implements ObservableState {

	private final Lock lock = new Lock() {};
	private final ObservableState state;
	private final boolean not;

	private @Nullable StateObserver observer;
	private @Nullable DefaultObservableState notObserver;

	DefaultObservableState(ObservableState state, boolean not) {
		this.state = requireNonNull(state);
		this.not = not;
	}

	@Override
	public String toString() {
		return Boolean.toString(is());
	}

	@Override
	public boolean is() {
		synchronized (lock) {
			return not ? !state.is() : state.is();
		}
	}

	@Override
	public ObservableState not() {
		if (not) {
			return state;
		}
		synchronized (lock) {
			if (notObserver == null) {
				notObserver = new DefaultObservableState(this, true);
			}

			return notObserver;
		}
	}

	@Override
	public Observer<Boolean> observer() {
		synchronized (lock) {
			if (observer == null) {
				observer = new StateObserver();
			}

			return observer;
		}
	}

	void notifyObservers(boolean newValue, boolean previousValue) {
		synchronized (lock) {
			if (previousValue != newValue) {
				if (observer != null) {
					observer.accept(newValue);
				}
				if (notObserver != null) {
					notObserver.notifyObservers(previousValue, newValue);
				}
			}
		}
	}

	private interface Lock {}

	private static final class StateObserver extends DefaultObserver<Boolean> {

		private void accept(Boolean data) {
			notifyListeners(data);
		}
	}
}
