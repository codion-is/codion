/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

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
    setDocumentCase(documentCase);
  }

  /**
   * @param documentCase the document case setting
   * @return this CaseDocumentFilter instance
   */
  public CaseDocumentFilter setDocumentCase(DocumentCase documentCase) {
    this.documentCase = requireNonNull(documentCase);
    return this;
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
