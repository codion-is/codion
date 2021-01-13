/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.util.Locale;

import static is.codion.swing.common.ui.textfield.StringLengthValidator.stringLengthValidator;

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
    super.setDocumentFilter(new SizedParsingDocumentFilter());
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
    ((SizedParsingDocumentFilter) getDocumentFilter()).setDocumentCase(documentCase);
  }

  /**
   * @return the document case setting
   */
  public DocumentCase getDocumentCase() {
    return ((SizedParsingDocumentFilter) getDocumentFilter()).documentCase;
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public int getMaxLength() {
    return ((StringLengthValidator) ((ParsingDocumentFilter<String>) getDocumentFilter()).getValidators().get(0)).getMaxLength();
  }

  /**
   * @param maxLength the maximum length of the text to allow, -1 if unlimited
   */
  public void setMaxLength(final int maxLength) {
    ((StringLengthValidator) ((ParsingDocumentFilter<String>) getDocumentFilter()).getValidators().get(0)).setMaxLength(maxLength);
  }

  private static final class SizedParsingDocumentFilter extends ParsingDocumentFilter<String> {

    private DocumentCase documentCase = DocumentCase.NONE;

    private SizedParsingDocumentFilter() {
      super(STRING_PARSER, stringLengthValidator());
    }

    @Override
    protected String transform(final String string) {
      return setCase(string);
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
