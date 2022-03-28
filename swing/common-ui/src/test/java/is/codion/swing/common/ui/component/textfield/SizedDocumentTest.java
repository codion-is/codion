/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.swing.common.ui.component.textfield.CaseDocumentFilter.DocumentCase;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SizedDocumentTest {

  @Test
  void test() throws BadLocationException {
    JTextField textField = new JTextField();
    SizedDocument document = new SizedDocument();
    textField.setDocument(document);
    textField.setText("hello");
    assertEquals("hello", textField.getText());

    document.setMaximumLength(10);
    assertEquals(10, document.getMaximumLength());

    textField.setText("hellohello");
    assertEquals("hellohello", textField.getText());

    assertThrows(IllegalArgumentException.class, () -> textField.setText("hellohellohello"));//invalid
    assertEquals("hellohello", textField.getText());

    document.getDocumentFilter().setDocumentCase(DocumentCase.UPPERCASE);
    assertEquals(DocumentCase.UPPERCASE, document.getDocumentFilter().getDocumentCase());

    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    document.getDocumentFilter().setDocumentCase(DocumentCase.LOWERCASE);
    assertEquals(DocumentCase.LOWERCASE, document.getDocumentFilter().getDocumentCase());

    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    document.getDocumentFilter().setDocumentCase(DocumentCase.NONE);

    document.insertString(2, "HOLA", null);
    assertEquals("heHOLAllo", textField.getText());
  }
}
