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
    final JTextField txt = new JTextField();
    final SizedDocument document = new SizedDocument();
    txt.setDocument(document);
    txt.setText("hello");
    assertEquals("hello", txt.getText());

    document.setMaxLength(10);
    assertEquals(10, document.getMaxLength());

    txt.setText("hellohello");
    assertEquals("hellohello", txt.getText());

    txt.setText("hellohellohello");//invalid
    assertEquals("hellohello", txt.getText());

    document.setDocumentCase(SizedDocument.DocumentCase.UPPERCASE);
    assertEquals(SizedDocument.DocumentCase.UPPERCASE, document.getDocumentCase());

    txt.setText("hello");
    assertEquals("HELLO", txt.getText());

    document.setDocumentCase(SizedDocument.DocumentCase.LOWERCASE);
    assertEquals(SizedDocument.DocumentCase.LOWERCASE, document.getDocumentCase());

    txt.setText("HELLO");
    assertEquals("hello", txt.getText());

    document.setDocumentCase(SizedDocument.DocumentCase.NONE);

    document.insertString(2, "HOLA", null);
    assertEquals("heHOLAllo", txt.getText());
  }
}
