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

/**
 * Manages listeners, notified each time something happens, without data.
 * <p>
 * An {@link is.codion.common.reactive.observer.Observer} is a sensor as well, its listeners notified along with its consumers, so anything that only
 * needs to know that something happened can listen to an event, a signal, a value or a state alike.
 * <p>
 * This interface provides default method implementations that delegate to {@link #sensor()}.
 * There are two ways to implement this interface:
 * <ul>
 *   <li>Extend {@link AbstractSensor} - provides concrete implementations where {@code sensor()} returns {@code this}</li>
 *   <li>Implement {@code sensor()} to return a delegate - useful for wrapper types</li>
 * </ul>
 * <p>All implementations are thread-safe and support concurrent access.</p>
 * @see is.codion.common.reactive.sensor.AbstractSensor
 * @see is.codion.common.reactive.observer.Observer
 * @see is.codion.common.reactive.event.Event
 */
public interface Sensor {

	/**
	 * Adds {@code listener} to this {@link Sensor}.
	 * Adding the same listener a second time has no effect.
	 * <p>Note that if the listener is already registered (whether strongly or via {@link #addWeakListener}) this
	 * returns false and retains the existing registration kind; it does not upgrade a weak registration to strong.
	 * @param listener the listener to add
	 * @return true if this sensor did not already contain the specified listener
	 * @throws NullPointerException in case listener is null
	 */
	default boolean addListener(Runnable listener) {
		return sensor().addListener(listener);
	}

	/**
	 * Removes {@code listener} from this {@link Sensor}, whether it was added via {@link #addListener(Runnable)}
	 * or {@link #addWeakListener(Runnable)}.
	 * @param listener the listener to remove
	 * @return true if this sensor contained the specified listener
	 */
	default boolean removeListener(Runnable listener) {
		return sensor().removeListener(listener);
	}

	/**
	 * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
	 * Adding the same listener a second time has no effect.
	 * <p>
	 * Note: Dead weak references accumulate until cleaned up, which happens automatically
	 * when listeners are added or removed. To trigger cleanup manually without modifying
	 * the listener set, call {@link #removeWeakListener(Runnable)} with any non-existing listener:
	 * {@snippet class = "is.codion.manual.javadoc.SensorSnippets" region = "removeWeakListener":
	 * // Clean up dead weak references // @start region=removeWeakListener
	 * sensor.removeWeakListener(() -> {}); // @end}
	 * @param listener the listener
	 * @return true if this sensor did not already contain the specified listener
	 */
	default boolean addWeakListener(Runnable listener) {
		return sensor().addWeakListener(listener);
	}

	/**
	 * Removes {@code listener} from this {@link Sensor}, whether it was added via {@link #addListener(Runnable)}
	 * or {@link #addWeakListener(Runnable)}.
	 * @param listener the listener to remove
	 * @return true if this sensor contained the specified listener
	 */
	default boolean removeWeakListener(Runnable listener) {
		return sensor().removeWeakListener(listener);
	}

	/**
	 * Returns the underlying {@link Sensor} to which listener operations are delegated.
	 * <p>
	 * For {@link AbstractSensor} and {@link is.codion.common.reactive.observer.AbstractObserver} subclasses this method returns {@code this}.
	 * For wrapper types, this returns the delegate sensor, and for an {@link is.codion.common.reactive.observer.Observer} implemented via
	 * {@link is.codion.common.reactive.observer.Observer#observer()}, its observer.
	 * @return the {@link Sensor} handling listener management
	 */
	Sensor sensor();
}
