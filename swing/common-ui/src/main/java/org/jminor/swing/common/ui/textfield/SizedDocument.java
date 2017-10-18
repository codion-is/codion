/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.util.Locale;

/**
 * A Document implementation which allows for setting the max text length and automatic conversion to upper or lower case.
 * Unfortunately for this document to function properly you must call document.setCaret(textField.getCaret());
 */
public class SizedDocument extends PlainDocument {

  /**
   * Specifies possible case conversions for document text.
   */
  public enum DocumentCase {
    NONE, UPPERCASE, LOWERCASE
  }

  /**
   * Instantiates a new SizedDocument
   */
  public SizedDocument() {
    setDocumentFilterInternal(new SizedDocumentFilter());
  }

  /**
   * Sets the caret, necessary for keeping the correct caret position when editing
   * @param caret the text field caret
   */
  public void setCaret(final Caret caret) {
    ((SizedDocumentFilter) getDocumentFilter()).setCaret(caret);
  }

  /**
   * @param filter the filter
   * @throws UnsupportedOperationException always
   */
  @Override
  public final void setDocumentFilter(final DocumentFilter filter) {
    throw new UnsupportedOperationException("Changing the DocumentFilter of SizedDocument and its descendants is not allowed");
  }

  /**
   * Sets the case setting for this document
   * @param documentCase the case setting
   */
  public final void setDocumentCase(final DocumentCase documentCase) {
    ((SizedDocumentFilter) getDocumentFilter()).setDocumentCase(documentCase);
  }

  /**
   * @return the document case setting
   */
  public final DocumentCase getDocumentCase() {
    return ((SizedDocumentFilter) getDocumentFilter()).getDocumentCase();
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public final int getMaxLength() {
    return ((SizedDocumentFilter) getDocumentFilter()).getMaxLength();
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public final void setMaxLength(final int maxLength) {
    ((SizedDocumentFilter) getDocumentFilter()).setMaxLength(maxLength);
  }

  /**
   * Sets the DocumentFilter
   * @param documentFilter the document filter
   */
  protected final void setDocumentFilterInternal(final DocumentFilter documentFilter) {
    super.setDocumentFilter(documentFilter);
  }

  /**
   * A DocumentFilter controlling both case and maximum length of the document content
   */
  protected static class SizedDocumentFilter extends DocumentFilter {

    private Caret caret;
    private DocumentCase documentCase = DocumentCase.NONE;
    private int maxLength = -1;

    /**
   * Sets the caret, necessary for keeping the correct caret position when editing
   * @param caret the text field caret
   */
  public void setCaret(final Caret caret) {
      this.caret = caret;
    }

    /**
     * @param documentCase the document case setting
     */
    public final void setDocumentCase(final DocumentCase documentCase) {
      this.documentCase = documentCase == null ? DocumentCase.NONE : documentCase;
    }

    /**
     * @return the document case setting
     */
    public final DocumentCase getDocumentCase() {
      return documentCase;
    }

    /**
     * @return the maximum length of the text to allow, -1 if unlimited
     */
    public final int getMaxLength() {
      return maxLength;
    }

    /**
     * @param maxLength the maximum length of the text to allow, -1 if unlimited
     */
    public final void setMaxLength(final int maxLength) {
      this.maxLength = maxLength < 0 ? -1 : maxLength;
    }

    @Override
    public final void insertString(final FilterBypass filterBypass, final int offset, final String string,
                                   final AttributeSet attributeSet) throws BadLocationException {
      replace(filterBypass, offset, 0, string, attributeSet);
    }

    @Override
    public final void replace(final FilterBypass filterBypass, final int offset, final int length, final String string,
                              final AttributeSet attributeSet) throws BadLocationException {
      final StringBuilder builder = getStringBuilder(filterBypass.getDocument());
      builder.replace(offset, offset + length, string);
      doReplace(filterBypass, builder.toString(), attributeSet, offset + string.length());
    }

    @Override
    public final void remove(final FilterBypass filterBypass, final int offset, final int length) throws BadLocationException {
      final StringBuilder builder = getStringBuilder(filterBypass.getDocument());
      builder.replace(offset, offset + length, "");
      doReplace(filterBypass, builder.toString(), null, offset);
    }

    /**
     * Performs any required transformations on the string before it is set as the document text.
     * @param string the string to transform
     * @return the transformed string or null if the value is invalid
     */
    protected String transformString(final String string) {
      switch (documentCase) {
        case UPPERCASE: return string.toUpperCase(Locale.getDefault());
        case LOWERCASE: return string.toLowerCase(Locale.getDefault());
        default: return string;
      }
    }

    private void doReplace(final FilterBypass filterBypass, final String string, final AttributeSet attributeSet, final int caretPosition)
            throws BadLocationException {
      final Document document = filterBypass.getDocument();
      final String transformedString = transformString(string);
      if (transformedString != null && (getMaxLength() < 0 || transformedString.length() <= getMaxLength())) {
        filterBypass.replace(0, document.getLength(), transformedString, attributeSet);
        if (caret != null) {
          caret.setDot(caretPosition);
        }
      }
    }

    private static StringBuilder getStringBuilder(final Document document) throws BadLocationException {
      return new StringBuilder(document.getText(0, document.getLength()));
    }
  }
}
