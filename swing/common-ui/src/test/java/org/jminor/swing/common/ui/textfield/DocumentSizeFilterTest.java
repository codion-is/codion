/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DocumentSizeFilterTest {

  @Test
  public void insert() {
    final JTextArea textArea = new JTextArea();
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(10));
    final String text8 = "12345678";
    textArea.setText(text8);
    assertEquals(text8, textArea.getText());
    textArea.insert("90", 8);
    final String text10 = "1234567890";
    assertEquals(text10, textArea.getText());
    textArea.insert(text10, 10);
    assertEquals(text10, textArea.getText());
  }

  @Test
  public void replace() {
    final JTextArea textArea = new JTextArea();
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(10));
    final String text8 = "12345678";
    textArea.setText(text8);
    textArea.replaceRange("90", 8, 8);
    final String text10 = "1234567890";
    assertEquals(text10, textArea.getText());
    textArea.replaceRange("ab", 4, 4);
    assertEquals(text10, textArea.getText());
  }
}
