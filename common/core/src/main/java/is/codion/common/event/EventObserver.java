/*
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
   * @return true if this observer did not already contain the specified listener
   * @throws NullPointerException in case listener is null
   */
  boolean addListener(Runnable listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   * @return true if this observer contained the specified listener
   */
  boolean removeListener(Runnable listener);

  /**
   * Adds {@code listener} to this {@link EventObserver}.
   * Adding the same listener a second time has no effect.
   * @param listener the listener to add
   * @return true if this observer did not already contain the specified listener
   * @throws NullPointerException in case listener is null
   */
  boolean addDataListener(Consumer<T> listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   * @return true if this observer contained the specified listener
   */
  boolean removeDataListener(Consumer<T> listener);

  /**
   * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
   * Adding the same listener a second time has no effect.
   * @param listener the listener
   * @return true if this observer did not already contain the specified listener
   */
  boolean addWeakListener(Runnable listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}
   * @param listener the listener to remove
   * @return true if this observer contained the specified listener
   */
  boolean removeWeakListener(Runnable listener);

  /**
   * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
   * Adding the same listener a second time has no effect.
   * @param listener the listener
   * @return true if this observer did not already contain the specified listener
   */
  boolean addWeakDataListener(Consumer<T> listener);

  /**
   * Removes {@code listener} from this {@link EventObserver}.
   * @param listener the listener to remove
   * @return true if this observer contained the specified listener
   */
  boolean removeWeakDataListener(Consumer<T> listener);
}
