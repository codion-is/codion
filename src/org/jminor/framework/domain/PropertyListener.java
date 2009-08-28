/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Used when listening to PropertyEvents
 */
public abstract class PropertyListener implements ActionListener {

  /** {@inheritDoc} */
  public final void actionPerformed(final ActionEvent event) {
    if (!(event instanceof PropertyEvent))
      throw new IllegalArgumentException("PropertyListener can only be used with PropertyEvent, " + event);

    propertyChanged((PropertyEvent) event);
  }

  protected abstract void propertyChanged(final PropertyEvent event);
}
