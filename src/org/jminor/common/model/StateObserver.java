package org.jminor.common.model;

import javax.swing.Action;

/**
 * User: darri
 * Date: 30.7.2010
 * Time: 12:02:28
 */
public interface StateObserver extends EventObserver {

  /**
   * @return true if the state being observed is active, false otherwise
   */
  boolean isActive();

  void addListeningAction(final Action action);

  /**
   * @return A StateObserver object that is always the reverse of the parent state
   */
  StateObserver getReversedState();
}
