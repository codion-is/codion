package org.jminor.common.model;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A simple document adapter.
 */
public class DocumentAdapter implements DocumentListener {

  /** {@inheritDoc} */
  public void changedUpdate(final DocumentEvent e) {}

  /** {@inheritDoc} */
  public void insertUpdate(final DocumentEvent e) {
    insertOrRemoveUpdate(e);
  }

  /** {@inheritDoc} */
  public void removeUpdate(final DocumentEvent e) {
    insertOrRemoveUpdate(e);
  }

  /**
   * Called during both insert and remove events.
   * @param e the document event
   */
  public void insertOrRemoveUpdate(final DocumentEvent e) {}
}
