/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * Base class for simple text field validation
 */
public abstract class ValidationDocumentFilter extends DocumentFilter {

  /** {@inheritDoc} */
  @Override
  public final void insertString(final FilterBypass filterBypass, final int offset, final String string,
                                 final AttributeSet attributeSet) throws BadLocationException {
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder();
    builder.append(document.getText(0, document.getLength()));
    builder.insert(offset, string);
    if (isValid(builder.toString())) {
      super.insertString(filterBypass, offset, string, attributeSet);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replace(final FilterBypass filterBypass, final int offset, final int length, final String text,
                            final AttributeSet attributeSet) throws BadLocationException {
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder();
    builder.append(document.getText(0, document.getLength()));
    builder.replace(offset, offset + length, text);
    if (isValid(builder.toString())) {
      super.replace(filterBypass, offset, length, text, attributeSet);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
    final Document document = filterBypass.getDocument();
    final StringBuilder builder = new StringBuilder();
    builder.append(document.getText(0, document.getLength()));
    builder.delete(offset, offset + length);
    if (isValid(builder.toString())) {
      super.remove(filterBypass, offset, length);
    }
  }

  /**
   * @param text the text to check
   * @return true if the given text is valid
   */
  protected abstract boolean isValid(final String text);
}
