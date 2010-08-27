package org.jminor.framework.client.model.event;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A listener for update events.
 */
public abstract class UpdateListener implements ActionListener {

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
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