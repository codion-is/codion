/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class MaximumTextFieldLength {

  MaximumTextFieldLength(Document document, int maximumLength) {
    requireNonNull(document);
    if (document instanceof SizedDocument) {
      ((SizedDocument) document).setMaximumLength(maximumLength);
    }
    else if (document instanceof AbstractDocument) {
      DocumentFilter documentFilter = ((AbstractDocument) document).getDocumentFilter();
      if (documentFilter == null) {
        CaseDocumentFilter caseDocumentFilter = CaseDocumentFilter.caseDocumentFilter();
        caseDocumentFilter.addValidator(new StringLengthValidator(maximumLength));
        ((AbstractDocument) document).setDocumentFilter(caseDocumentFilter);
      }
      else if (documentFilter instanceof CaseDocumentFilter) {
        CaseDocumentFilter caseDocumentFilter = (CaseDocumentFilter) documentFilter;
        Optional<StringLengthValidator> lengthValidator = caseDocumentFilter.getValidators().stream()
                .filter(StringLengthValidator.class::isInstance)
                .map(StringLengthValidator.class::cast)
                .findFirst();
        if (lengthValidator.isPresent()) {
          lengthValidator.get().setMaximumLength(maximumLength);
        }
        else {
          caseDocumentFilter.addValidator(new StringLengthValidator(maximumLength));
        }
      }
    }
  }
}
