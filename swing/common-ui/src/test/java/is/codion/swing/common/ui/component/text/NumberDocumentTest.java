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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.text.BadLocationException;

import static java.text.NumberFormat.getIntegerInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class NumberDocumentTest {

	@Test
	void test() throws BadLocationException {
		NumberDocument<Integer> document = new NumberDocument<>(getIntegerInstance(), Integer.class);

		document.getDocumentFilter().setSilentValidation(true);// Dont throw, just prevent input

		document.getDocumentFilter().setMinimumValue(0);
		document.getDocumentFilter().setMaximumValue(1);
		document.insertString(0, "-", null);
		assertEquals(0, document.getLength()); // input prevented
		assertNull(document.get()); // invalid

		clear(document);

		document.getDocumentFilter().setMinimumValue(-1);
		document.getDocumentFilter().setMaximumValue(1);
		document.insertString(0, "-", null);
		assertEquals(-1, document.get());

		clear(document);

		document.getDocumentFilter().setMinimumValue(-1);
		document.getDocumentFilter().setMaximumValue(0);
		document.insertString(0, "-", null);
		assertEquals(-1, document.get());
		document.insertString(1, "2", null); // -2 invalid
		assertEquals(-1, document.get());

		clear(document);

		document.getDocumentFilter().setMinimumValue(-3);
		document.getDocumentFilter().setMaximumValue(0);
		document.insertString(0, "-", null);
		assertEquals(-1, document.get());
		document.insertString(1, "2", null); // -2 valid
		assertEquals(-2, document.get());
		document.insertString(1, "3", null); // -23 invalid
		assertEquals(-2, document.get());
	}

	private static void clear(NumberDocument<Integer> document) throws BadLocationException {
		document.remove(0, document.getLength());
	}
}
