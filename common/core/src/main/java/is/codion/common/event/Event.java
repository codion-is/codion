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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.event;

import is.codion.common.observable.Observer;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * <p>An event class implementing {@link Observer}.
 * <p>Events are triggered with {@link #run()} or {@link #accept(Object)}, depending
 * on whether data should be propagated with the event.
 * <p>Listeners are notified in the order they were added.
 * <p>Note that <b>both</b> listeners and consumers are notified each time the event is
 * triggered, regardless of whether {@link #run()} or {@link #accept(Object)} is used.
 * <p>Events provide access to their {@link Observer} instance via {@link #observer()}, which can
 * be used to add listeners and consumers, but can not be used to trigger the event.
 * <p>Unhandled exceptions occurring in a listener will prevent further listeners from being notified.
 * <p><b>Thread Safety:</b> Listener and consumer management (add/remove) is thread-safe.
 * However, event triggering via {@link #run()} or {@link #accept(Object)} is NOT thread-safe
 * and should be performed from a single thread (such as an application UI thread).</p>
 * {@snippet :
 * Event<Boolean> event = Event.event();
 *
 * event.addListener(this::doSomething);
 *
 * event.run();
 *
 * event.addConsumer(this::onBoolean);
 *
 * event.accept(true);
 *
 * Observer<Boolean> observer = event.observer();
 *
 * observer.addListener(this::doSomethingElse);
 *}
 * <p>Listeners and Consumers can be added using a {@link java.lang.ref.WeakReference}.
 * {@snippet :
 * observer.addWeakListener(this::doSomethingElse);
 * observer.addWeakConsumer(this::onBoolean);
 *}
 * <p>Any weak references that no longer refer to a listener/consumer instance
 * are cleared when listeners or consumers are added or removed.
 * <p>A factory for {@link Event} instances via {@link #event()}.
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