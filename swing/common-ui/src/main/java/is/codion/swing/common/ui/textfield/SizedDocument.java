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
    this(-1);
  }

  /**
   * Instantiates a new SizedDocument
   * @param maximumLength the maximum text length
   */
  public SizedDocument(final int maximumLength) {
    super.setDocumentFilter(new SizedParsingDocumentFilter());
    setMaximumLength(maximumLength);
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
  public int getMaximumLength() {
    return ((SizedParsingDocumentFilter) getDocumentFilter()).getStringLengthValidator().getMaximumLength();
  }

  /**
   * @param maximumLength the maximum length of the text to allow, -1 if unlimited
   */
  public void setMaximumLength(final int maximumLength) {
    ((SizedParsingDocumentFilter) getDocumentFilter()).getStringLengthValidator().setMaximumLength(maximumLength);
  }

  private static final class SizedParsingDocumentFilter extends ParsingDocumentFilter<String> {

    private final StringLengthValidator lengthValidator;

    private DocumentCase documentCase = DocumentCase.NONE;

    private SizedParsingDocumentFilter() {
      this(stringLengthValidator());
    }

    private SizedParsingDocumentFilter(final StringLengthValidator lengthValidator) {
      super(STRING_PARSER, lengthValidator);
      this.lengthValidator = lengthValidator;
    }

    @Override
    protected String transform(final String string) {
      return setCase(string);
    }

    private StringLengthValidator getStringLengthValidator() {
      return lengthValidator;
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
