/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.Toolkit;

/**
 * A DocumentFilter restricting the maximum length of the string the document can contain
 */
public final class DocumentSizeFilter extends DocumentFilter {

  private final int maxLength;

  /**
   * @param maxLength the maximum length of the string the document can contain
   */
  public DocumentSizeFilter(final int maxLength) {
    this.maxLength = maxLength;
  }

  /** {@inheritDoc} */
  @Override
  public void insertString(final FilterBypass fb, final int offs, final String str, final AttributeSet a) throws BadLocationException {
    if ((fb.getDocument().getLength() + str.length()) <= maxLength) {
      super.insertString(fb, offs, str, a);
    }
    else {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void replace(final FilterBypass fb, final int offs, final int length, final String str, final AttributeSet a) throws BadLocationException {
    if ((fb.getDocument().getLength() + str.length() - length) <= maxLength) {
      super.replace(fb, offs, length, str, a);
    }
    else {
      Toolkit.getDefaultToolkit().beep();
    }
  }
}
