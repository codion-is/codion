/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Used when listening to PropertyChangeEvents
 */
public abstract class PropertyListener implements ActionListener {

  /** {@inheritDoc} */
  public final void actionPerformed(final ActionEvent e) {
    if (!(e instanceof PropertyChangeEvent))
      throw new IllegalArgumentException("PropertyListener used improperly, " + e);

    propertyChanged((PropertyChangeEvent) e);
  }

  protected abstract void propertyChanged(final PropertyChangeEvent e);
}
