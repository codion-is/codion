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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
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
