/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.textfield;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A simple document adapter, combining the {@code insertUpdate} and {@code removeUpdate} into {@code contentsChanged}
 */
public abstract class DocumentAdapter implements DocumentListener {

  /** {@inheritDoc} */
  @Override
  public void changedUpdate(final DocumentEvent e) {}

  /** {@inheritDoc} */
  @Override
  public final void insertUpdate(final DocumentEvent e) {
    contentsChanged(e);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeUpdate(final DocumentEvent e) {
    contentsChanged(e);
  }

  /**
   * Called when the contents of this document change, either via insertion, update or removal
   * @param e the document event
   */
  public abstract void contentsChanged(DocumentEvent e);
}
