package org.jminor.common.model;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends EventObserver, EventInfoObserver {

  /**
   * @return true if the state being observed is active, false otherwise
   */
  boolean isActive();

  /**
   * @return an EventObserver notified each time the observed state changes
   */
  EventObserver getStateChangeObserver();

  /**
   * @return A StateObserver object that is always the reverse of the parent state
   */
  StateObserver getReversedObserver();

  /**
   * @param listener a listener notified each time the observed state is activated
   */
  void addActivateListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeActivateListener(final EventListener listener);

  /**
   * @param listener a listener notified each time the observed state is deactivated
   */
  void addDeactivateListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeDeactivateListener(final EventListener listener);
}
