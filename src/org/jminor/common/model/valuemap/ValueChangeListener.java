/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
   * Used when listening to ValueChangeEvents
 */
public abstract class ValueChangeListener<T, V> implements ActionListener {

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public final void actionPerformed(final ActionEvent event) {
    if (!(event instanceof ValueChangeEvent))
      throw new IllegalArgumentException("ValueChangeListener can only be used with ValueChangeEvent, " + event);

    valueChanged((ValueChangeEvent<T, V>) event);
  }

  protected abstract void valueChanged(final ValueChangeEvent<T, V> event);
}
