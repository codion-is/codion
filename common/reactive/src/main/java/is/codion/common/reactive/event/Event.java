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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.event;

import is.codion.common.reactive.sensor.Sensor;

/**
 * <p>An event, notifying its listeners each time it occurs, without data. For an event carrying data, see {@link is.codion.common.reactive.signal.Signal}.
 * <p>An event is triggered with {@link #run()}, notifying its listeners in the order they were added.
 * <p>Events provide access to their {@link Sensor} via {@link #sensor()}, which can be used to add listeners,
 * but can not be used to trigger the event.
 * <p>Unhandled exceptions occurring in a listener will prevent further listeners from being notified.
 * <p><b>Thread Safety:</b> Listener management (add/remove) is thread-safe.
 * However, triggering via {@link #run()} is NOT thread-safe
 * and should be performed from a single thread (such as an application UI thread).</p>
 * {@snippet class = "is.codion.manual.javadoc.EventSnippets" region = "usage":
 * Event event = Event.event(); // @start region=usage
 *
 * event.addListener(this::doSomething);
 *
 * event.run();
 *
 * Sensor sensor = event.sensor();
 *
 * sensor.addListener(this::doSomethingElse); // @end}
 * <p>Listeners can be added using a {@link java.lang.ref.WeakReference}, so they must be strongly
 * referenced elsewhere, a lambda or method reference only referenced by the sensor being garbage collected right away.
 * {@snippet class = "is.codion.manual.javadoc.EventSnippets" region = "weak":
 * // listener is a field // @start region=weak
 * sensor.addWeakListener(listener); // @end}
 * <p>Any weak references that no longer refer to a listener instance
 * are cleared when listeners are added or removed.
 * <p>A factory for {@link Event} instances via {@link #event()}.
 * @see is.codion.common.reactive.signal.Signal
 */
public interface Event extends Runnable, Sensor {

	/**
	 * Triggers this event, notifying its listeners.
	 */
	@Override
	void run();

	/**
	 * @return a sensor notified each time this event occurs, which can not trigger it
	 */
	@Override
	Sensor sensor();

	/**
	 * Creates a new {@link Event}.
	 * @return a new Event
	 */
	static Event event() {
		return new DefaultEvent();
	}
}
