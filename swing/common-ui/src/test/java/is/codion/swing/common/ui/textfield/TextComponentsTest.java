/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

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
    final JTextField textField = new JTextField();
    final SizedDocument document = new SizedDocument();
    textField.setDocument(document);
    TextComponents.maximumLength(document, 5);
    assertThrows(IllegalArgumentException.class, () -> textField.setText("123456"));
    TextComponents.maximumLength(document, 3);
    textField.setText("123");
    assertThrows(IllegalArgumentException.class, () -> textField.setText("1234"));
    assertEquals(1, document.getDocumentFilter().getValidators().size());
    TextComponents.maximumLength(document, -1);
    textField.setText("123456789");
  }

  @Test
  void maximumLengthTextArea() {
    final JTextArea textArea = new JTextArea();
    final Document document = textArea.getDocument();
    TextComponents.maximumLength(document, 5);
    assertThrows(IllegalArgumentException.class, () -> textArea.setText("123456"));
    TextComponents.maximumLength(document, 3);
    textArea.setText("123");
    assertThrows(IllegalArgumentException.class, () -> textArea.setText("1234"));
    final DocumentFilter documentFilter = ((AbstractDocument) document).getDocumentFilter();
    assertTrue(documentFilter instanceof CaseDocumentFilter);
    assertEquals(1, ((CaseDocumentFilter) documentFilter).getValidators().size());
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
    textField.setDocument(new SizedDocument());
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
    textField.setDocument(new SizedDocument());
    TextComponents.lowerCase(textField.getDocument());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());
  }

  @Test
  void selectAllOnFocusGained() {
    final JTextField textField = new JTextField("test");
    final int focusListenerCount = textField.getFocusListeners().length;
    TextComponents.selectAllOnFocusGained(textField);
    assertEquals(focusListenerCount + 1, textField.getFocusListeners().length);
    TextComponents.selectNoneOnFocusGained(textField);
    assertEquals(focusListenerCount, textField.getFocusListeners().length);
  }

  @Test
  void enableNullTextField() {
    assertThrows(NullPointerException.class, () -> TextFieldHint.create(null, "test"));
  }

  @Test
  void enableNullHintString() {
    assertThrows(IllegalArgumentException.class, () -> TextFieldHint.create(new JTextField(), null));
  }

  @Test
  void enableEmptyHintString() {
    assertThrows(IllegalArgumentException.class, () -> TextFieldHint.create(new JTextField(), ""));
  }

  @Test
  void hint() {
    final JTextField textField = new JTextField();
    final TextFieldHint hint = TextFieldHint.create(textField, "search");
    assertEquals("search", hint.getHintText());
    assertEquals("search", textField.getText());
    textField.setText("he");
    assertEquals("he", textField.getText());
  }
}
