/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.textfield;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A simple document adapter, with default implementations for the {@link #insertUpdate(DocumentEvent)} and
 * {@link #removeUpdate(DocumentEvent)} calling {@link #contentsChanged(DocumentEvent)}.
 */
public interface DocumentAdapter extends DocumentListener {

  /** {@inheritDoc} */
  @Override
  default void changedUpdate(final DocumentEvent e) {}

  /** {@inheritDoc} */
  @Override
  default void insertUpdate(final DocumentEvent e) {
    contentsChanged(e);
  }

  /** {@inheritDoc} */
  @Override
  default void removeUpdate(final DocumentEvent e) {
    contentsChanged(e);
  }

  /**
   * Called when the contents of this document change, either via insertion, update or removal
   * @param e the document event
   */
  void contentsChanged(DocumentEvent e);
}
