package org.jminor.common.model;

import javax.swing.Action;

/**
 * Specifies a State observer.
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
  StateObserver getReversedState();
}
