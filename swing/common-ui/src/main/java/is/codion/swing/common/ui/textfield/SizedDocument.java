/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.ui.textfield.CaseDocumentFilter.DocumentCase;

import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * A Document implementation which allows for setting the max text length and automatic conversion to upper or lower case.
 */
public final class SizedDocument extends PlainDocument {

  private final CaseDocumentFilter documentFilter;
  private final StringLengthValidator stringLengthValidator;

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
    documentFilter = CaseDocumentFilter.caseDocumentFilter(DocumentCase.NONE);
    stringLengthValidator = new StringLengthValidator(maximumLength);
    documentFilter.addValidator(stringLengthValidator);
    super.setDocumentFilter(documentFilter);
  }

  /**
   * @param filter the filter
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setDocumentFilter(final DocumentFilter filter) {
    throw new UnsupportedOperationException("Changing the DocumentFilter of SizedDocument is not allowed");
  }

  @Override
  public CaseDocumentFilter getDocumentFilter() {
    return (CaseDocumentFilter) super.getDocumentFilter();
  }

  /**
   * @return the maximum length of the text to allow, -1 if unlimited
   */
  public int getMaximumLength() {
    return stringLengthValidator.getMaximumLength();
  }

  /**
   * @param maximumLength the maximum length of the text to allow, -1 if unlimited
   */
  public void setMaximumLength(final int maximumLength) {
    stringLengthValidator.setMaximumLength(maximumLength);
  }
}
