/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.util.Locale;

/**
 * A Document implementation which allows for setting the max text length and automatic conversion to upper or lower case.
 */
public final class SizedDocument extends PlainDocument {

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
    super.setDocumentFilter(new SizedDocumentFilter());
  }

  /**
   * @param filter the filter
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setDocumentFilter(final DocumentFilter filter) {
    throw new UnsupportedOperationException("Changing the DocumentFilter of SizedDocument is not allowed");
  }

  /**
   * Sets the case setting for this document
   * @param documentCase the case setting
   */
  public void setDocumentCase(final DocumentCase documentCase) {
    ((SizedDocumentFilter) getDocumentFilter()).setDocumentCase(documentCase);
  }

  /**
   * @return the document case setting
   */
  public DocumentCase getDocumentCase() {
    return ((SizedDocumentFilter) getDocumentFilter()).documentCase;
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public int getMaxLength() {
    return ((SizedDocumentFilter) getDocumentFilter()).maxLength;
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public void setMaxLength(final int maxLength) {
    ((SizedDocumentFilter) getDocumentFilter()).setMaxLength(maxLength);
  }

  /**
   * A DocumentFilter controlling both case and maximum length of the document content
   */
  private static final class SizedDocumentFilter extends DocumentFilter {

    private DocumentCase documentCase = DocumentCase.NONE;
    private int maxLength = -1;

    @Override
    public void insertString(final FilterBypass filterBypass, final int offset, final String string,
                             final AttributeSet attributeSet) throws BadLocationException {
      replace(filterBypass, offset, 0, string, attributeSet);
    }

    @Override
    public void replace(final FilterBypass filterBypass, final int offset, final int length, final String string,
                        final AttributeSet attributeSet) throws BadLocationException {
      final String caseFixed = setCase(string);
      final StringBuilder builder = new StringBuilder(filterBypass.getDocument().getText(0, filterBypass.getDocument().getLength()));
      builder.replace(offset, offset + length, caseFixed);
      if (maxLength < 0 || builder.length() <= maxLength) {
        super.replace(filterBypass, offset, length, caseFixed, attributeSet);
      }
    }

    /**
     * @param documentCase the document case setting
     */
    private void setDocumentCase(final DocumentCase documentCase) {
      this.documentCase = documentCase == null ? DocumentCase.NONE : documentCase;
    }

    /**
     * @param maxLength the maximum length of the text to allow, -1 if unlimited
     */
    private void setMaxLength(final int maxLength) {
      this.maxLength = maxLength < 0 ? -1 : maxLength;
    }

    private String setCase(final String string) {
      switch (documentCase) {
        case UPPERCASE: return string.toUpperCase(Locale.getDefault());
        case LOWERCASE: return string.toLowerCase(Locale.getDefault());
        default: return string;
      }
    }
  }
}
