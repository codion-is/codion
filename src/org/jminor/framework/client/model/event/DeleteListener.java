package org.jminor.framework.client.model.event;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A listener for delete events.
 */
public abstract class DeleteListener implements ActionListener {

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
    if (!(e instanceof DeleteEvent)) {
      throw new IllegalArgumentException("DeleteListener can only be used with DeleteEvent, " + e);
    }

    deleted((DeleteEvent) e);
  }

  /**
   * Handles the given delete event
   * @param event the event to handle
   */
  protected abstract void deleted(final DeleteEvent event);
}