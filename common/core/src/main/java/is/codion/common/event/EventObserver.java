/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

/**
 * Manages listeners for an Event.
 * @param <T> the type of data propagated with the event.
 */
public interface EventObserver<T> {

  /**
   * Adds {@code listener} to this {@link EventObserver}.
   * Adding the same listener a second time has no effect.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addListener(EventListener listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeListener(EventListener listener);

  /**
   * Adds {@code listener} to this {@link EventObserver}.
   * Adding the same listener a second time has no effect.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addDataListener(EventDataListener<T> listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeDataListener(EventDataListener<T> listener);

  /**
   * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
   * Adding the same listener a second time has no effect.
   * @param listener the listener
   */
  void addWeakListener(EventListener listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeWeakListener(EventListener listener);

  /**
   * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
   * Adding the same listener a second time has no effect.
   * @param listener the listener
   */
  void addWeakDataListener(EventDataListener<T> listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeWeakDataListener(EventDataListener<T> listener);
}
