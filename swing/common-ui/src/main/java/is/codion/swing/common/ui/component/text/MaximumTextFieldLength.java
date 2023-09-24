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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

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
        Optional<StringLengthValidator> lengthValidator = caseDocumentFilter.validators().stream()
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
