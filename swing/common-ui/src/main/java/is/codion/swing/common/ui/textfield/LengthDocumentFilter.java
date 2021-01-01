/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A DocumentFilter restricting the maximum length of the string the document can contain
 */
public final class LengthDocumentFilter extends DocumentFilter {

  private final int maxLength;

  /**
   * @param maxLength the maximum length of the string the document can contain
   */
  public LengthDocumentFilter(final int maxLength) {
    this.maxLength = maxLength;
  }

  @Override
  public void insertString(final FilterBypass fb, final int offs, final String str, final AttributeSet a) throws BadLocationException {
    if ((fb.getDocument().getLength() + str.length()) <= maxLength) {
      super.insertString(fb, offs, str, a);
    }
  }

  @Override
  public void replace(final FilterBypass fb, final int offs, final int length, final String str, final AttributeSet a) throws BadLocationException {
    if ((fb.getDocument().getLength() + str.length() - length) <= maxLength) {
      super.replace(fb, offs, length, str, a);
    }
  }
}
