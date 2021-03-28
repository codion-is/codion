/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.textfield;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A document adapter, with default implementations for the {@link #insertUpdate(DocumentEvent)} and
 * {@link #removeUpdate(DocumentEvent)} calling {@link #contentsChanged(DocumentEvent)}.
 */
public interface DocumentAdapter extends DocumentListener {

  @Override
  default void changedUpdate(final DocumentEvent e) {}

  @Override
  default void insertUpdate(final DocumentEvent e) {
    contentsChanged(e);
  }

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
