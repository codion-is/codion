package org.jminor.framework.client.model.event;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class InsertListener implements ActionListener {

  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
    if (!(e instanceof InsertEvent)) {
      throw new IllegalArgumentException("InsertListener can only be used with InsertEvent, " + e);
    }

    inserted((InsertEvent) e);
  }

  protected abstract void inserted(final InsertEvent event);
}
