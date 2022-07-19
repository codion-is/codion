/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import static java.util.Objects.requireNonNull;

final class TextFieldDocumentCase {

  TextFieldDocumentCase(Document document, CaseDocumentFilter.DocumentCase documentCase) {
    requireNonNull(document);
    if (document instanceof SizedDocument) {
      ((SizedDocument) document).getDocumentFilter().setDocumentCase(documentCase);
    }
    else if (document instanceof AbstractDocument) {
      DocumentFilter documentFilter = ((AbstractDocument) document).getDocumentFilter();
      if (documentFilter == null) {
        CaseDocumentFilter caseDocumentFilter = CaseDocumentFilter.caseDocumentFilter();
        caseDocumentFilter.setDocumentCase(documentCase);
        ((AbstractDocument) document).setDocumentFilter(caseDocumentFilter);
      }
      else if (documentFilter instanceof CaseDocumentFilter) {
        ((CaseDocumentFilter) documentFilter).setDocumentCase(documentCase);
      }
    }
  }
}
