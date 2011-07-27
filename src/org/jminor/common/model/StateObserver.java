package org.jminor.common.model;

import javax.swing.Action;
import java.awt.event.ActionListener;

/**
 * Specifies an observer for a {@link State} instance.
 */
public interface StateObserver extends EventObserver {

  /**
   * @return true if the state being observed is active, false otherwise
   */
  boolean isActive();

  /**
   * Links the enabled state of the given action to the active state of the state being observed
   * @param action an action to link
   */
  void addListeningAction(final Action action);

  /**
   * @return A StateObserver object that is always the reverse of the parent state
   */
  StateObserver getReversedObserver();

  /**
   * @param listener a listener notified each time this state is activated
   */
  void addActivateListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeActivateListener(final ActionListener listener);

  /**
   * @param listener a listener notified each time this state is deactivated
   */
  void addDeactivateListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeDeactivateListener(final ActionListener listener);
}
