package org.jminor.framework.client.model.event;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class UpdateListener implements ActionListener {

  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent e) {
    if (!(e instanceof UpdateEvent)) {
      throw new IllegalArgumentException("UpdateListener can only be used with UpdateEvent, " + e);
    }

    updated((UpdateEvent) e);
  }

  protected abstract void updated(final UpdateEvent event);
}