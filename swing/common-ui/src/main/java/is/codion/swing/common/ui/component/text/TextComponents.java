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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import java.awt.Dimension;

/**
 * A utility class for JTextComponents.
 */
public final class TextComponents {

	private static Dimension preferredTextFieldSize;

	private TextComponents() {}

	/**
	 * Sets the maximum length for the given document, supports {@link SizedDocument} and {@link AbstractDocument}
	 * @param document the document
	 * @param maximumLength the maximum string length
	 */
	public static void maximumLength(Document document, int maximumLength) {
		new MaximumTextFieldLength(document, maximumLength);
	}

	/**
	 * Makes the given document convert all lower case input to upper case,
	 * supports {@link SizedDocument} and {@link AbstractDocument}
	 * @param document the document
	 */
	public static void upperCase(Document document) {
		new TextFieldDocumentCase(document, CaseDocumentFilter.DocumentCase.UPPERCASE);
	}

	/**
	 * Makes the given document convert all upper case input to lower case,
	 * supports {@link SizedDocument} and {@link AbstractDocument}
	 * @param document the document
	 */
	public static void lowerCase(Document document) {
		new TextFieldDocumentCase(document, CaseDocumentFilter.DocumentCase.LOWERCASE);
	}

	/**
	 * @return the preferred size of a JTextField
	 */
	public static synchronized Dimension preferredTextFieldSize() {
		if (preferredTextFieldSize == null) {
			preferredTextFieldSize = new JTextField().getPreferredSize();
		}

		return preferredTextFieldSize;
	}

	/**
	 * @return the preferred height of a JTextField
	 */
	public static int preferredTextFieldHeight() {
		return preferredTextFieldSize().height;
	}
}
