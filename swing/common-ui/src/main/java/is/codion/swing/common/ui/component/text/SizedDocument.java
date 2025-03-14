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
 * Copyright (c) 2012 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * A Document implementation which allows for setting the max text length and automatic conversion to upper or lower case.
 * For instances use the {@link #sizedDocument()} or {@link #sizedDocument(int)} factory methods.
 * @see #sizedDocument()
 * @see #sizedDocument(int)
 */
public final class SizedDocument extends PlainDocument {

	private final CaseDocumentFilter documentFilter;
	private final StringLengthValidator stringLengthValidator;

	private SizedDocument(int maximumLength) {
		documentFilter = CaseDocumentFilter.caseDocumentFilter();
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
	public void setMaximumLength(int maximumLength) {
		stringLengthValidator.setMaximumLength(maximumLength);
	}

	/**
	 * @return a new {@link SizedDocument}
	 */
	public static SizedDocument sizedDocument() {
		return sizedDocument(-1);
	}

	/**
	 * @param maximumLength the maximum text length
	 * @return a new {@link SizedDocument}
	 */
	public static SizedDocument sizedDocument(int maximumLength) {
		return new SizedDocument(maximumLength);
	}
}
