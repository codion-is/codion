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
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observable;

final class ObservableIsPresent<T> implements Runnable {

	private final Observable<T> observable;
	private final State state = State.state();

	ObservableIsPresent(Observable<T> observable) {
		this.observable = observable;
		this.state.set(observable.optional().isPresent());
		observable.addListener(this);
	}

	@Override
	public void run() {
		state.set(observable.optional().isPresent());
	}

	ObservableState observable() {
		return state.observable();
	}
}
