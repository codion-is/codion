/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SizedDocumentTest {

  @Test
  public void test() throws BadLocationException {
    final JTextField textField = new JTextField();
    final SizedDocument document = new SizedDocument();
    textField.setDocument(document);
    textField.setText("hello");
    assertEquals("hello", textField.getText());

    document.setMaxLength(10);
    assertEquals(10, document.getMaxLength());

    textField.setText("hellohello");
    assertEquals("hellohello", textField.getText());

    textField.setText("hellohellohello");//invalid
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
