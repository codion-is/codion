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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import static org.junit.jupiter.api.Assertions.*;

public class TextComponentsTest {

  @Test
  void maximumLengthTextField() {
    JTextField textField = new JTextField();
    SizedDocument document = SizedDocument.sizedDocument();
    textField.setDocument(document);
    TextComponents.maximumLength(document, 5);
    assertThrows(IllegalArgumentException.class, () -> textField.setText("123456"));
    TextComponents.maximumLength(document, 3);
    textField.setText("123");
    assertThrows(IllegalArgumentException.class, () -> textField.setText("1234"));
    assertEquals(1, document.getDocumentFilter().validators().size());
    TextComponents.maximumLength(document, -1);
    textField.setText("123456789");
  }

  @Test
  void maximumLengthTextArea() {
    JTextArea textArea = new JTextArea();
    Document document = textArea.getDocument();
    TextComponents.maximumLength(document, 5);
    assertThrows(IllegalArgumentException.class, () -> textArea.setText("123456"));
    TextComponents.maximumLength(document, 3);
    textArea.setText("123");
    assertThrows(IllegalArgumentException.class, () -> textArea.setText("1234"));
    DocumentFilter documentFilter = ((AbstractDocument) document).getDocumentFilter();
    assertTrue(documentFilter instanceof CaseDocumentFilter);
    assertEquals(1, ((CaseDocumentFilter) documentFilter).validators().size());
    TextComponents.maximumLength(document, -1);
    textArea.setText("123456789");
  }

  @Test
  void upperCase() {
    JTextField textField = new JTextField();
    TextComponents.upperCase(textField.getDocument());
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    textField = new JTextField();
    textField.setDocument(SizedDocument.sizedDocument());
    TextComponents.upperCase(textField.getDocument());
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());
  }

  @Test
  void lowerCase() {
    JTextField textField = new JTextField();
    TextComponents.lowerCase(textField.getDocument());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    textField = new JTextField();
    textField.setDocument(SizedDocument.sizedDocument());
    TextComponents.lowerCase(textField.getDocument());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());
  }

  @Test
  void selectAllOnFocusGained() {
    JTextField textField = new JTextField("test");
    int focusListenerCount = textField.getFocusListeners().length;
    TextComponents.selectAllOnFocusGained(textField);
    assertEquals(focusListenerCount + 1, textField.getFocusListeners().length);
    TextComponents.selectNoneOnFocusGained(textField);
    assertEquals(focusListenerCount, textField.getFocusListeners().length);
  }
}
