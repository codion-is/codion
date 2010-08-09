package org.jminor.common.model;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class DocumentAdapter implements DocumentListener {

  /** {@inheritDoc} */
  public void changedUpdate(final DocumentEvent e) {}

  /** {@inheritDoc} */
  public void insertUpdate(final DocumentEvent e) {
    insertOrUpdate(e);
  }

  /** {@inheritDoc} */
  public void removeUpdate(final DocumentEvent e) {
    insertOrUpdate(e);
  }

  public void insertOrUpdate(final DocumentEvent e) {}
}
