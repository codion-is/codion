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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultStateObserver implements StateObserver {

	private final Object lock = new Object();
	private final StateObserver observedState;
	private final boolean not;

	private Event<Boolean> stateChangedEvent;
	private DefaultStateObserver notObserver;

	DefaultStateObserver(StateObserver observedState, boolean not) {
		this.observedState = requireNonNull(observedState);
		this.not = not;
	}

	@Override
	public String toString() {
		return Boolean.toString(get());
	}

	@Override
	public Boolean get() {
		synchronized (lock) {
			return not ? !observedState.get() : observedState.get();
		}
	}

	@Override
	public boolean isNull() {
		return false;
	}

	@Override
	public boolean isNotNull() {
		return true;
	}

	@Override
	public boolean nullable() {
		return false;
	}

	@Override
	public StateObserver not() {
		if (not) {
			return observedState;
		}
		synchronized (lock) {
			if (notObserver == null) {
				notObserver = new DefaultStateObserver(this, true);
			}

			return notObserver;
		}
	}

	@Override
	public boolean addListener(Runnable listener) {
		return eventObserver().addListener(listener);
	}

	@Override
	public boolean removeListener(Runnable listener) {
		return eventObserver().removeListener(listener);
	}

	@Override
	public boolean addDataListener(Consumer<? super Boolean> listener) {
		return eventObserver().addDataListener(listener);
	}

	@Override
	public boolean removeDataListener(Consumer<? super Boolean> listener) {
		return eventObserver().removeDataListener(listener);
	}

	@Override
	public boolean addWeakListener(Runnable listener) {
		return eventObserver().addWeakListener(listener);
	}

	@Override
	public boolean removeWeakListener(Runnable listener) {
		return eventObserver().removeWeakListener(listener);
	}

	@Override
	public boolean addWeakDataListener(Consumer<? super Boolean> listener) {
		return eventObserver().addWeakDataListener(listener);
	}

	@Override
	public boolean removeWeakDataListener(Consumer<? super Boolean> listener) {
		return eventObserver().removeWeakDataListener(listener);
	}

	void notifyObservers(boolean newValue, boolean previousValue) {
		synchronized (lock) {
			if (previousValue != newValue) {
				if (stateChangedEvent != null) {
					stateChangedEvent.accept(newValue);
				}
				if (notObserver != null) {
					notObserver.notifyObservers(previousValue, newValue);
				}
			}
		}
	}

	private EventObserver<Boolean> eventObserver() {
		synchronized (lock) {
			if (stateChangedEvent == null) {
				stateChangedEvent = Event.event();
			}

			return stateChangedEvent.observer();
		}
	}
}
