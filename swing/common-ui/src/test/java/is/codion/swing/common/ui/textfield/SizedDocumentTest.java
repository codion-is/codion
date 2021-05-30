/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SizedDocumentTest {

  @Test
  void test() throws BadLocationException {
    final JTextField textField = new JTextField();
    final SizedDocument document = new SizedDocument();
    textField.setDocument(document);
    textField.setText("hello");
    assertEquals("hello", textField.getText());

    document.setMaximumLength(10);
    assertEquals(10, document.getMaximumLength());

    textField.setText("hellohello");
    assertEquals("hellohello", textField.getText());

    assertThrows(IllegalArgumentException.class, () -> textField.setText("hellohellohello"));//invalid
    assertEquals("hellohello", textField.getText());

    document.setDocumentCase(SizedDocument.DocumentCase.UPPERCASE);
    assertEquals(SizedDocument.DocumentCase.UPPERCASE, document.getDocumentCase());

    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    document.setDocumentCase(SizedDocument.DocumentCase.LOWERCASE);
    assertEquals(SizedDocument.DocumentCase.LOWERCASE, document.getDocumentCase());

    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    document.setDocumentCase(SizedDocument.DocumentCase.NONE);

    document.insertString(2, "HOLA", null);
    assertEquals("heHOLAllo", textField.getText());
  }
}
