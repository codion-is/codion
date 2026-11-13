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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observable;

import is.codion.common.reactive.internal.Listeners;
import is.codion.common.reactive.observer.AbstractObserver;
import is.codion.common.reactive.observer.Observer;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static is.codion.common.reactive.observable.Change.change;
import static java.util.Objects.deepEquals;

/**
 * Subscribes to the observable only while this observer, {@link #previous()} or {@link #current()} has listeners or
 * consumers, subscribing on the first and unsubscribing on the last, as a conditional does. Since the subscription is
 * what keeps it reachable, an instance never listened to, or whose listeners and consumers are all removed, is left
 * to be collected, rather than accumulating on the observable, {@link Observable#changed()} creating one per call.
 * @param <T> the value type
 */
final class DefaultChangeObserver<T> extends AbstractObserver<Change<T>>
				implements ChangeObserver<T>, Listeners.Hooks, Listeners.Superseding {

	private final Observable<T> observable;
	private final Runnable notifyChange = this::notifyChange;
	private final ValueObserver previous = new ValueObserver();
	private final ValueObserver current = new ValueObserver();
	private final Set<Object> listened = new HashSet<>(3);

	private @Nullable T value;
	//the changes notified, a nested one included
	private int changes = 0;

	DefaultChangeObserver(Observable<T> observable) {
		this.observable = observable;
	}

	@Override
	public Observer<T> previous() {
		return previous;
	}

	@Override
	public Observer<T> current() {
		return current;
	}

	@Override
	public void onFirstListener() {
		listened(this, true);
	}

	@Override
	public void onLastListener() {
		listened(this, false);
	}

	/**
	 * @param part this observer, previous() or current()
	 * @param listened true if the part gained its first listener, false if it lost its last
	 */
	private synchronized void listened(Object part, boolean listened) {
		boolean subscribed = !this.listened.isEmpty();
		if (listened) {
			this.listened.add(part);
		}
		else {
			this.listened.remove(part);
		}
		if (!subscribed && !this.listened.isEmpty()) {
			//the value as it is now, the previous value of the first change from here on
			value = observable.get();
			observable.addListener(notifyChange);
		}
		else if (subscribed && this.listened.isEmpty()) {
			observable.removeListener(notifyChange);
		}
	}

	//a listener rather than a consumer, reading the current value, not the one notified, which a listener before
	//it may have changed since, the change to the new one then reported already
	private void notifyChange() {
		T newValue = observable.get();
		T previousValue;
		synchronized (this) {
			previousValue = value;
			//an equal value replaces the one held without notifying, so previous() is the latest instance notified
			value = newValue;
		}
		if (!deepEquals(previousValue, newValue)) {
			int change = ++changes;
			notifyListeners(change(previousValue, newValue));
			//unless superseded by a nested change, which notified them already
			if (change == changes) {
				previous.notify(previousValue);
			}
			if (change == changes) {
				current.notify(newValue);
			}
		}
	}

	private final class ValueObserver extends AbstractObserver<T> implements Listeners.Hooks, Listeners.Superseding {

		@Override
		public void onFirstListener() {
			listened(this, true);
		}

		@Override
		public void onLastListener() {
			listened(this, false);
		}

		private void notify(@Nullable T value) {
			notifyListeners(value);
		}
	}
}
