/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.textfield;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A document adapter, with default implementations for the {@link #insertUpdate(DocumentEvent)} and
 * {@link #removeUpdate(DocumentEvent)} calling {@link #contentsChanged(DocumentEvent)}.
 */
public interface DocumentAdapter extends DocumentListener {

  @Override
  default void changedUpdate(DocumentEvent e) {}

  @Override
  default void insertUpdate(DocumentEvent e) {
    contentsChanged(e);
  }

  @Override
  default void removeUpdate(DocumentEvent e) {
    contentsChanged(e);
  }

  /**
   * Called when the contents of this document change, either via insertion, update or removal
   * @param e the document event
   */
  void contentsChanged(DocumentEvent e);
}
