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
 * Copyright (c) 2012 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringLengthValidatorTest {

  @Test
  void insert() {
    JTextArea textArea = new JTextArea();
    ParsingDocumentFilter<String> documentFilter = new ParsingDocumentFilter<>(ParsingDocumentFilter.STRING_PARSER);
    documentFilter.addValidator(new StringLengthValidator(10));
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentFilter);
    final String text8 = "12345678";
    textArea.setText(text8);
    assertEquals(text8, textArea.getText());
    textArea.insert("90", 8);
    final String text10 = "1234567890";
    assertEquals(text10, textArea.getText());
    assertThrows(IllegalArgumentException.class, () -> textArea.insert(text10, 10));
    assertEquals(text10, textArea.getText());
  }

  @Test
  void replace() {
    JTextArea textArea = new JTextArea();
    ParsingDocumentFilter<String> documentFilter = new ParsingDocumentFilter<>(ParsingDocumentFilter.STRING_PARSER);
    documentFilter.addValidator(new StringLengthValidator(10));
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentFilter);
    final String text8 = "12345678";
    textArea.setText(text8);
    textArea.replaceRange("90", 8, 8);
    final String text10 = "1234567890";
    assertEquals(text10, textArea.getText());
    assertThrows(IllegalArgumentException.class, () -> textArea.replaceRange("ab", 4, 4));
    assertEquals(text10, textArea.getText());
  }
}
