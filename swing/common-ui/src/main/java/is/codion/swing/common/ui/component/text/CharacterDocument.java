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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.text.CaseDocumentFilter.DocumentCase;
import is.codion.swing.common.ui.component.text.ParsingDocumentFilter.SilentValidator;

import org.jspecify.annotations.Nullable;

import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

final class CharacterDocument extends PlainDocument {

	private final CaseDocumentFilter documentFilter;

	CharacterDocument() {
		documentFilter = new CaseDocumentFilter(DocumentCase.NONE);
		documentFilter.addValidator(new CharacterLengthValidator<>());
		super.setDocumentFilter(documentFilter);
	}

	@Override
	public void setDocumentFilter(DocumentFilter filter) {
		throw new UnsupportedOperationException("Changing the DocumentFilter of CharacterDocument is not allowed");
	}

	private static final class CharacterLengthValidator<T> implements SilentValidator<T> {

		@Override
		public void validate(@Nullable T text) {
			if (text != null && text.toString().length() > 1) {
				throw new IllegalArgumentException();
			}
		}
	}
}
