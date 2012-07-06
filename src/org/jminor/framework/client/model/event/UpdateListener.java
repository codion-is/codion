package org.jminor.framework.client.model.event;


import org.jminor.common.model.EventListener;

import java.awt.event.ActionEvent;

/**
 * A listener for update events.
 */
public abstract class UpdateListener implements EventListener {

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked"})
  public final void eventOccurred(final ActionEvent e) {
    if (!(e instanceof UpdateEvent)) {
      throw new IllegalArgumentException("UpdateListener can only be used with UpdateEvent, " + e);
    }

    updated((UpdateEvent) e);
  }

  /**
   * Handles the given update event
   * @param event the event to handle
   */
  protected abstract void updated(final UpdateEvent event);
}