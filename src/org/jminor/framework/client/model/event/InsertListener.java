package org.jminor.framework.client.model.event;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A listener for insert events.
 */
public abstract class InsertListener implements ActionListener {

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
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
