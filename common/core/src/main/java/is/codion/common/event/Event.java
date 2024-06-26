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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.event;

import java.util.function.Consumer;

/**
 * An event class. Listeners are notified in the order they were added.
 * <pre>
 * Event&lt;Boolean&gt; event = Event.event();
 *
 * EventObserver&lt;Boolean&gt; observer = event.observer();
 *
 * observer.addListener(this::doSomething);
 * observer.addConsumer(this::onBoolean);
 *
 * event.accept(true);
 * </pre>
 * A factory for {@link Event} instances.
 * @param <T> the type of data propagated with this event
 */
public interface Event<T> extends Runnable, Consumer<T>, EventObserver<T> {

	/**
	 * Triggers this event.
	 */
	@Override
	void run();

	/**
	 * Triggers this event.
	 * @param data data associated with the event
	 */
	@Override
	void accept(T data);

	/**
	 * @return an observer notified each time this event occurs
	 */
	EventObserver<T> observer();

	/**
	 * Creates a new {@link Event}.
	 * @param <T> the type of data propagated to listeners on event occurrence
	 * @return a new Event
	 */
	static <T> Event<T> event() {
		return new DefaultEvent<>();
	}
}