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

import is.codion.common.observable.Observer;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * An event class implementing the {@link Observer} interface.
 * <pre>
 * {@code
 * Event<Boolean> event = Event.event();
 *
 * event.addListener(this::doSomething);
 *
 * event.run();
 *
 * event.addConsumer(this::onBoolean);
 *
 * event.accept(true);
 * }
 * </pre>
 * The event observer observes the event but can not trigger it.
 * <pre>
 * {@code
 * Observer<Boolean> observer = event.observer();
 *
 * observer.addListener(this::doSomethingElse);
 * }
 * </pre>
 *
 * Listeners and Consumers can be added using a {@link java.lang.ref.WeakReference}.
 * <pre>
 * {@code
 * observer.addWeakListener(this::doSomethingElse);
 * observer.addWeakConsumer(this::onBoolean);
 * }
 * </pre>
 * Any weak references that no longer refer to a listener/consumer instance
 * are cleared when listeners are added or removed, but to manually clear these empty
 * weak references call {@link #removeWeakListener(Runnable)} or {@link #removeWeakConsumer(Consumer)}
 * with a listener/consumer instance which has not been registered on the observer, such as a new one.
 * <pre>
 * {@code
 * observer.removeWeakListener(() -> {});
 * observer.removeWeakConsumer(value -> {});
 * }
 * </pre>
 * A factory for {@link Event} instances via {@link #event()}.
 * @param <T> the type of data propagated with this event
 */
public interface Event<T> extends Runnable, Consumer<T>, Observer<T> {

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
	void accept(@Nullable T data);

	/**
	 * @return an observer notified each time this event occurs
	 */
	Observer<T> observer();

	/**
	 * Creates a new {@link Event}.
	 * @param <T> the type of data propagated to listeners on event occurrence
	 * @return a new Event
	 */
	static <T> Event<T> event() {
		return new DefaultEvent<>();
	}
}