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
package is.codion.common.reactive.signal;

import is.codion.common.reactive.observer.Observer;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * <p>A signal, notifying its listeners and consumers each time it occurs, carrying data. For an event without data,
 * see {@link is.codion.common.reactive.event.Event}.
 * <p>A signal is triggered with {@link #accept(Object)}, notifying its listeners and consumers in the order they were
 * added, the consumers receiving the data.
 * <p>Signals provide access to their {@link Observer} via {@link #observer()}, which can be used to add listeners
 * and consumers, but can not be used to trigger the signal.
 * <p>Unhandled exceptions occurring in a listener or consumer will prevent further ones from being notified.
 * <p><b>Thread Safety:</b> Listener and consumer management (add/remove) is thread-safe.
 * However, triggering via {@link #accept(Object)} is NOT thread-safe
 * and should be performed from a single thread (such as an application UI thread).</p>
 * {@snippet class = "is.codion.manual.javadoc.SignalSnippets" region = "usage":
 * Signal<Boolean> signal = Signal.signal(); // @start region=usage
 *
 * signal.addListener(this::doSomething);
 *
 * signal.addConsumer(this::onBoolean);
 *
 * signal.accept(true);
 *
 * Observer<Boolean> observer = signal.observer();
 *
 * observer.addConsumer(this::onBooleanElsewhere); // @end}
 * <p>Listeners and consumers can be added using a {@link java.lang.ref.WeakReference}, so they must be strongly
 * referenced elsewhere, a lambda or method reference only referenced by the observer being garbage collected right away.
 * {@snippet class = "is.codion.manual.javadoc.SignalSnippets" region = "weak":
 * // listener and consumer are fields // @start region=weak
 * observer.addWeakListener(listener);
 * observer.addWeakConsumer(consumer); // @end}
 * <p>Any weak references that no longer refer to a listener or consumer instance
 * are cleared when listeners or consumers are added or removed.
 * <p>A factory for {@link Signal} instances via {@link #signal()}.
 * @param <T> the type of data propagated with this signal
 * @see is.codion.common.reactive.event.Event
 */
public interface Signal<T> extends Consumer<T>, Observer<T> {

	/**
	 * Triggers this signal, notifying its listeners, and its consumers with the given data.
	 * @param data data associated with the signal
	 */
	@Override
	void accept(@Nullable T data);

	/**
	 * @return an observer notified each time this signal occurs, which can not trigger it
	 */
	@Override
	Observer<T> observer();

	/**
	 * Creates a new {@link Signal}.
	 * @param <T> the type of data propagated with the signal
	 * @return a new Signal
	 */
	static <T> Signal<T> signal() {
		return new DefaultSignal<>();
	}
}
