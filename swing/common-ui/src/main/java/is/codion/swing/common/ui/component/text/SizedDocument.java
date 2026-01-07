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
 * Copyright (c) 2012 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.text.CaseDocumentFilter.DocumentCase;

import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

final class SizedDocument extends PlainDocument {

	private final CaseDocumentFilter documentFilter;
	private final StringLengthValidator stringLengthValidator;

	SizedDocument(int maximumLength) {
		documentFilter = new CaseDocumentFilter(DocumentCase.NONE);
		stringLengthValidator = new StringLengthValidator(maximumLength);
		documentFilter.addValidator(stringLengthValidator);
		super.setDocumentFilter(documentFilter);
	}

	/**
	 * @param filter the filter
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public void setDocumentFilter(DocumentFilter filter) {
		throw new UnsupportedOperationException("Changing the DocumentFilter of SizedDocument is not allowed");
	}

	/**
	 * @return the maximum length of the text to allow, -1 if unlimited
	 */
	int getMaximumLength() {
		return stringLengthValidator.getMaximumLength();
	}

	/**
	 * @param maximumLength the maximum length of the text to allow, -1 if unlimited
	 */
	void setMaximumLength(int maximumLength) {
		stringLengthValidator.setMaximumLength(maximumLength);
	}
}
