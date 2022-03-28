/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

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
   * @param documentCase the document case setting
   */
  private CaseDocumentFilter(DocumentCase documentCase) {
    super(STRING_PARSER);
    this.documentCase = documentCase;
  }

  /**
   * @param documentCase the document case setting
   */
  public void setDocumentCase(DocumentCase documentCase) {
    this.documentCase = documentCase == null ? DocumentCase.NONE : documentCase;
  }

  /**
   * @return the document case setting
   */
  public DocumentCase getDocumentCase() {
    return documentCase;
  }

  /**
   * Creates a new CaseDocumentFilter instance, configured with {@link DocumentCase#NONE}
   * @return a new CaseDocumentFilter instance
   */
  public static CaseDocumentFilter caseDocumentFilter() {
    return new CaseDocumentFilter(DocumentCase.NONE);
  }

  /**
   * Creates a new CaseDocumentFilter instance, configured with the given document case
   * @param documentCase the document case
   * @return a new CaseDocumentFilter instance
   */
  public static CaseDocumentFilter caseDocumentFilter(DocumentCase documentCase) {
    return new CaseDocumentFilter(documentCase);
  }

  @Override
  protected String transform(String string) {
    return string == null ? null : setCase(string);
  }

  private String setCase(String string) {
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
