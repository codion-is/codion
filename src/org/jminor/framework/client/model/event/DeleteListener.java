package org.jminor.framework.client.model.event;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class DeleteListener implements ActionListener {

  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
    if (!(e instanceof DeleteEvent)) {
      throw new IllegalArgumentException("DeleteListener can only be used with DeleteEvent, " + e);
    }

    deleted((DeleteEvent) e);
  }

  protected abstract void deleted(final DeleteEvent event);
}