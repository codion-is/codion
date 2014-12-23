/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A simple document adapter, combining the <code>insertUpdate</code> and <code>removeUpdate</code> into <code>contentsChanged</code>
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
  public abstract void contentsChanged(final DocumentEvent e);
}
