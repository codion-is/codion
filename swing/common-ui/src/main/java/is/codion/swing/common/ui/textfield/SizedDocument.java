/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

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
    super.setDocumentFilter(new CaseDocumentFilter());
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
    ((CaseDocumentFilter) getDocumentFilter()).setDocumentCase(documentCase);
  }

  /**
   * @return the document case setting
   */
  public DocumentCase getDocumentCase() {
    return ((CaseDocumentFilter) getDocumentFilter()).documentCase;
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public int getMaxLength() {
    return ((CaseDocumentFilter) getDocumentFilter()).getMaxLength();
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public void setMaxLength(final int maxLength) {
    ((CaseDocumentFilter) getDocumentFilter()).setMaxLength(maxLength);
  }

  /**
   * A DocumentFilter controlling both case and maximum length of the document content
   */
  private static final class CaseDocumentFilter extends LengthDocumentFilter {

    private DocumentCase documentCase = DocumentCase.NONE;

    @Override
    protected ParseResult<String> parse(final String text) {
      final String correctedText = setCase(text);

      return parseResult(correctedText, correctedText);
    }

    /**
     * @param documentCase the document case setting
     */
    private void setDocumentCase(final DocumentCase documentCase) {
      this.documentCase = documentCase == null ? DocumentCase.NONE : documentCase;
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
