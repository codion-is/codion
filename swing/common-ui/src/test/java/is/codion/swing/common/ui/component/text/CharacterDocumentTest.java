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

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class CharacterDocumentTest {

	@Test
	void test() {
		JTextField textField = new JTextField();
		textField.setDocument(new CharacterDocument());
		textField.replaceSelection("a");
		assertEquals("a", textField.getText());
		textField.replaceSelection("b");// typing, rejected silently
		assertEquals("a", textField.getText());
		textField.setText("b");
		assertEquals("b", textField.getText());
		assertThrows(IllegalArgumentException.class, () -> textField.setText("bc"));
		assertEquals("b", textField.getText());
		assertThrows(IllegalArgumentException.class, () -> textField.replaceSelection("cd"));// a paste
		assertEquals("b", textField.getText());
		// the maximum length of a character document is fixed
		TextComponents.maximumLength(textField.getDocument(), 5);
		textField.replaceSelection("c");
		assertEquals("b", textField.getText());
	}
}
