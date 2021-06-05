/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import java.util.Locale;

/**
 * A DocumentFilter implementation with automatic conversion to upper or lower case.
 */
public final class CaseDocumentFilter extends ParsingDocumentFilter<String> {

  /**
   * Specifies possible case conversions for document text.
   */
  public enum DocumentCase {
    NONE, UPPERCASE, LOWERCASE;
  }

  private DocumentCase documentCase = DocumentCase.NONE;

  /**
   * Instantiates a new CaseDocumentFilter
   */
  public CaseDocumentFilter() {
    this(DocumentCase.NONE);
  }

  /**
   * Instantiates a new CaseDocumentFilter
   * @param documentCase the document case setting
   */
  public CaseDocumentFilter(final DocumentCase documentCase) {
    super(STRING_PARSER);
    this.documentCase = documentCase;
  }

  /**
   * @param documentCase the document case setting
   */
  public void setDocumentCase(final DocumentCase documentCase) {
    this.documentCase = documentCase == null ? DocumentCase.NONE : documentCase;
  }

  /**
   * @return the document case setting
   */
  public DocumentCase getDocumentCase() {
    return documentCase;
  }

  @Override
  protected String transform(final String string) {
    return setCase(string);
  }

  private String setCase(final String string) {
    switch (documentCase) {
      case UPPERCASE:
        return string.toUpperCase(Locale.getDefault());
      case LOWERCASE:
        return string.toLowerCase(Locale.getDefault());
      default:
        return string;
    }
  }
}
