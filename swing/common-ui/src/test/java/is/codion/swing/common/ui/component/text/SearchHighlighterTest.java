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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class SearchHighlighterTest {

	@Test
	void test() throws BadLocationException {
		Document document = new DefaultStyledDocument();
		document.insertString(0, "Hello there, here we are", null);

		JTextArea textArea = new JTextArea(document);

		SearchHighlighter highlighter = SearchHighlighter.searchHighlighter(textArea);
		highlighter.highlightColor(Color.BLUE);
		highlighter.highlightSelectedColor(Color.MAGENTA);

		JTextField searchField = highlighter.createSearchField();

		searchField.setText("th");
		assertEquals(6, highlighter.selectedHighlightPosition());

		searchField.setText(null);
		assertNull(highlighter.selectedHighlightPosition());

		searchField.setText("re");
		assertEquals(9, highlighter.selectedHighlightPosition());

		highlighter.nextSearchPosition();
		assertEquals(15, highlighter.selectedHighlightPosition());

		highlighter.nextSearchPosition();
		assertEquals(22, highlighter.selectedHighlightPosition());

		highlighter.nextSearchPosition();
		assertEquals(9, highlighter.selectedHighlightPosition());

		highlighter.previousSearchPosition();
		assertEquals(22, highlighter.selectedHighlightPosition());

		highlighter.previousSearchPosition();
		assertEquals(15, highlighter.selectedHighlightPosition());

		highlighter.previousSearchPosition();
		assertEquals(9, highlighter.selectedHighlightPosition());

		highlighter.previousSearchPosition();
		assertEquals(22, highlighter.selectedHighlightPosition());

		highlighter.caseSensitive().set(true);
		highlighter.searchString().set("he");
		assertEquals(7, highlighter.selectedHighlightPosition());

		highlighter.caseSensitive().set(false);
		assertEquals(0, highlighter.selectedHighlightPosition());

		highlighter.caseSensitive().set(true);
		highlighter.searchString().set("He");
		highlighter.nextSearchPosition();
		assertEquals(0, highlighter.selectedHighlightPosition());

		highlighter.previousSearchPosition();
		assertEquals(0, highlighter.selectedHighlightPosition());
	}
}
