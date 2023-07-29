/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import java.util.function.Consumer;

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
  void addListener(Runnable listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeListener(Runnable listener);

  /**
   * Adds {@code listener} to this {@link EventObserver}.
   * Adding the same listener a second time has no effect.
   * @param listener the listener to add
   * @throws NullPointerException in case listener is null
   */
  void addDataListener(Consumer<T> listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeDataListener(Consumer<T> listener);

  /**
   * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
   * Adding the same listener a second time has no effect.
   * @param listener the listener
   */
  void addWeakListener(Runnable listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeWeakListener(Runnable listener);

  /**
   * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
   * Adding the same listener a second time has no effect.
   * @param listener the listener
   */
  void addWeakDataListener(Consumer<T> listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   */
  void removeWeakDataListener(Consumer<T> listener);
}
