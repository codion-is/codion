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

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SizedDocumentTest {

	@Test
	void test() throws BadLocationException {
		JTextField textField = new JTextField();
		SizedDocument document = new SizedDocument(-1);
		textField.setDocument(document);
		textField.setText("hello");
		assertEquals("hello", textField.getText());

		document.setMaximumLength(10);
		assertEquals(10, document.getMaximumLength());

		textField.setText("hellohello");
		assertEquals("hellohello", textField.getText());

		assertThrows(IllegalArgumentException.class, () -> textField.setText("hellohellohello"));//invalid
		assertEquals("hellohello", textField.getText());

		CaseDocumentFilter documentFilter = (CaseDocumentFilter) document.getDocumentFilter();
		documentFilter.setDocumentCase(DocumentCase.UPPERCASE);

		textField.setText("hello");
		assertEquals("HELLO", textField.getText());

		documentFilter.setDocumentCase(DocumentCase.LOWERCASE);

		textField.setText("HELLO");
		assertEquals("hello", textField.getText());

		documentFilter.setDocumentCase(DocumentCase.NONE);

		document.insertString(2, "HOLA", null);
		assertEquals("heHOLAllo", textField.getText());
	}
}
