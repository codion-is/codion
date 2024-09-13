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
package is.codion.common.event;

import is.codion.common.observer.Observer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultObserver<T> implements Observer<T> {

	private static final String CONSUMER = "consumer";
	private static final String LISTENER = "listener";

	private final Object lock = new Object();

	private Set<Runnable> listeners;
	private Set<Consumer<? super T>> consumers;
	private List<WeakReference<Runnable>> weakListeners;
	private List<WeakReference<Consumer<? super T>>> weakConsumers;

	@Override
	public boolean addConsumer(Consumer<? super T> consumer) {
		requireNonNull(consumer, CONSUMER);
		synchronized (lock) {
			return initConsumers().add(consumer);
		}
	}

	@Override
	public boolean removeConsumer(Consumer<? super T> consumer) {
		requireNonNull(consumer, CONSUMER);
		synchronized (lock) {
			return initConsumers().remove(consumer);
		}
	}

	@Override
	public boolean addListener(Runnable listener) {
		requireNonNull(listener, LISTENER);
		synchronized (lock) {
			return initListeners().add(listener);
		}
	}

	@Override
	public boolean removeListener(Runnable listener) {
		requireNonNull(listener, LISTENER);
		synchronized (lock) {
			return initListeners().remove(listener);
		}
	}

	@Override
	public boolean addWeakListener(Runnable listener) {
		requireNonNull(listener, LISTENER);
		synchronized (lock) {
			List<WeakReference<Runnable>> references = initWeakListeners();
			for (WeakReference<Runnable> reference : references) {
				if (reference.refersTo(listener)) {
					return false;
				}
			}
			return references.add(new WeakReference<>(listener));
		}
	}

	@Override
	public boolean removeWeakListener(Runnable listener) {
		requireNonNull(listener, LISTENER);
		synchronized (lock) {
			List<WeakReference<Runnable>> references = initWeakListeners();
			references.removeIf(reference -> reference.get() == null);

			return references.removeIf(reference -> reference.refersTo(listener));
		}
	}

	@Override
	public boolean addWeakConsumer(Consumer<? super T> consumer) {
		requireNonNull(consumer, CONSUMER);
		synchronized (lock) {
			List<WeakReference<Consumer<? super T>>> references = initWeakConsumers();
			for (WeakReference<Consumer<? super T>> reference : references) {
				if (reference.refersTo(consumer)) {
					return false;
				}
			}
			return references.add(new WeakReference<>(consumer));
		}
	}

	@Override
	public boolean removeWeakConsumer(Consumer<? super T> consumer) {
		requireNonNull(consumer, CONSUMER);
		synchronized (lock) {
			List<WeakReference<Consumer<? super T>>> references = initWeakConsumers();
			references.removeIf(reference -> reference.get() == null);

			return references.removeIf(reference -> reference.refersTo(consumer));
		}
	}

	void notifyListeners(T data) {
		for (Runnable listener : listeners()) {
			listener.run();
		}
		for (Consumer<? super T> consumer : consumers()) {
			consumer.accept(data);
		}
		for (WeakReference<Runnable> reference : weakListeners()) {
			Runnable weakListener = reference.get();
			if (weakListener != null) {
				weakListener.run();
			}
		}
		for (WeakReference<Consumer<? super T>> reference : weakConsumers()) {
			Consumer<? super T> consumer = reference.get();
			if (consumer != null) {
				consumer.accept(data);
			}
		}
	}

	private List<Runnable> listeners() {
		synchronized (lock) {
			if (listeners != null && !listeners.isEmpty()) {
				return new ArrayList<>(listeners);
			}
		}

		return emptyList();
	}

	private List<Consumer<? super T>> consumers() {
		synchronized (lock) {
			if (consumers != null && !consumers.isEmpty()) {
				return new ArrayList<>(consumers);
			}
		}

		return emptyList();
	}

	private List<WeakReference<Runnable>> weakListeners() {
		synchronized (lock) {
			if (weakListeners != null && !weakListeners.isEmpty()) {
				weakListeners.removeIf(reference -> reference.get() == null);

				return new ArrayList<>(weakListeners);
			}
		}

		return emptyList();
	}

	private List<WeakReference<Consumer<? super T>>> weakConsumers() {
		synchronized (lock) {
			if (weakConsumers != null && !weakConsumers.isEmpty()) {
				weakConsumers.removeIf(reference -> reference.get() == null);

				return new ArrayList<>(weakConsumers);
			}
		}

		return emptyList();
	}

	private Set<Runnable> initListeners() {
		if (listeners == null) {
			listeners = new LinkedHashSet<>(1);
		}

		return listeners;
	}

	private Set<Consumer<? super T>> initConsumers() {
		if (consumers == null) {
			consumers = new LinkedHashSet<>(1);
		}

		return consumers;
	}

	private List<WeakReference<Runnable>> initWeakListeners() {
		if (weakListeners == null) {
			weakListeners = new ArrayList<>(1);
		}

		return weakListeners;
	}

	private List<WeakReference<Consumer<? super T>>> initWeakConsumers() {
		if (weakConsumers == null) {
			weakConsumers = new ArrayList<>(1);
		}

		return weakConsumers;
	}
}
