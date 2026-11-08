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
package is.codion.common.reactive.sensor;

import is.codion.common.reactive.internal.Listeners;
import is.codion.common.reactive.observer.AbstractObserver;

/**
 * Thread-safe base implementation of {@link Sensor}.
 * <p>
 * Subclasses trigger notifications by calling {@link #notifyListeners()}, which notifies the listeners in the
 * order they were added.
 * <p>
 * The {@link #sensor()} method returns {@code this}, so the default methods
 * in {@link Sensor} resolve directly to the concrete implementations here.
 * <p>
 * All listener management operations are thread-safe.
 * Dead weak references are cleaned up during add/remove operations.
 * @see Sensor
 * @see AbstractObserver
 */
public abstract class AbstractSensor implements Sensor {

	private final Listeners listeners = new Listeners(this);

	/**
	 * Instantiates a new {@link AbstractSensor}
	 */
	protected AbstractSensor() {}

	@Override
	public final boolean addListener(Runnable listener) {
		return listeners.addListener(listener);
	}

	@Override
	public final boolean removeListener(Runnable listener) {
		return listeners.remove(listener);
	}

	@Override
	public final boolean addWeakListener(Runnable listener) {
		return listeners.addWeakListener(listener);
	}

	@Override
	public final boolean removeWeakListener(Runnable listener) {
		return listeners.remove(listener);
	}

	@Override
	public final Sensor sensor() {
		return this;
	}

	/**
	 * Notifies the listeners, in the order they were added
	 */
	protected final void notifyListeners() {
		listeners.notifyListeners();
	}
}
