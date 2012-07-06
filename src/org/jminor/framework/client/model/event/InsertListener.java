package org.jminor.framework.client.model.event;


import org.jminor.common.model.EventListener;

import java.awt.event.ActionEvent;

/**
 * A listener for insert events.
 */
public abstract class InsertListener implements EventListener {

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked"})
  public final void eventOccurred(final ActionEvent e) {
    if (!(e instanceof InsertEvent)) {
      throw new IllegalArgumentException("InsertListener can only be used with InsertEvent, " + e);
    }

    inserted((InsertEvent) e);
  }

  /**
   * Handles the given insert event
   * @param event the event to handle
   */
  protected abstract void inserted(final InsertEvent event);
}
