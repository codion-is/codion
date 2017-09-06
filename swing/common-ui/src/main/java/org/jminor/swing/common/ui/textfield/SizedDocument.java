/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.util.Locale;

/**
 * A Document implementation which allows for setting the max text length
 * and automatic conversion to upper or lower case.
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

    private DocumentCase documentCase = DocumentCase.NONE;
    private int maxLength = -1;

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
    public final void insertString(final FilterBypass fb, final int offset, final String string,
                                   final AttributeSet attributeSet) throws BadLocationException {
      final Document document = fb.getDocument();
      final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
      builder.insert(offset, string);
      if (getMaxLength() < 0 || builder.length() <= getMaxLength()) {
        doReplace(fb, builder, offset, string.length(), attributeSet);
      }
    }

    @Override
    public final void replace(final FilterBypass fb, final int offset, final int length, final String string,
                              final AttributeSet attributeSet) throws BadLocationException {
      final Document document = fb.getDocument();
      final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
      builder.replace(offset, offset + length, string);
      if (getMaxLength() < 0 || builder.length() <= getMaxLength()) {
        doReplace(fb, builder, offset, string.length(), attributeSet);
      }
    }

    @Override
    public final void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException {
      final Document document = fb.getDocument();
      final StringBuilder builder = new StringBuilder(document.getText(0, document.getLength()));
      builder.replace(offset, offset + length, "");
      doReplace(fb, builder, offset, length, null);
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

    private void doReplace(final FilterBypass fb, final StringBuilder builder, final int offset,
                           final int stringLength, final AttributeSet attributeSet)
            throws BadLocationException {
      final Document document = fb.getDocument();
      final String transformedString = transformString(builder.toString());
      if (transformedString != null) {
        final String replacement = removeCommonSuffix(transformedString, document.getText(0,
                document.getLength()), offset, stringLength);
        fb.replace(0, document.getLength() - (transformedString.length() - replacement.length()),
                replacement, attributeSet);
      }
    }

    //We remove the common suffix if any, to preserve the caret position
    private String removeCommonSuffix(final String replacementText, final String currentText, final int offset,
                                      final int length) {
      final StringBuilder replacement = new StringBuilder(replacementText);
      final StringBuilder current = new StringBuilder(currentText);
      while (replacement.length() > 0 && current.length() > 0 &&
              replacement.charAt(replacement.length() - 1) == current.charAt(current.length() - 1)) {
        replacement.replace(replacement.length() - 1, replacement.length(), "");
        current.replace(current.length() - 1, current.length(), "");
      }

      return replacement.toString();
    }
  }
}
