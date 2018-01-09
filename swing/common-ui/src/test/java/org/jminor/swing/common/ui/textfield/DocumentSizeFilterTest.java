/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;

import static org.junit.Assert.assertEquals;

public class DocumentSizeFilterTest {

  @Test
  public void insert() {
    final JTextArea textArea = new JTextArea();
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(10));
    final String txt8 = "12345678";
    textArea.setText(txt8);
    assertEquals(txt8, textArea.getText());
    textArea.insert("90", 8);
    final String txt10 = "1234567890";
    assertEquals(txt10, textArea.getText());
    textArea.insert(txt10, 10);
    assertEquals(txt10, textArea.getText());
  }

  @Test
  public void replace() {
    final JTextArea textArea = new JTextArea();
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(new DocumentSizeFilter(10));
    final String txt8 = "12345678";
    textArea.setText(txt8);
    textArea.replaceRange("90", 8, 8);
    final String txt10 = "1234567890";
    assertEquals(txt10, textArea.getText());
    textArea.replaceRange("ab", 4, 4);
    assertEquals(txt10, textArea.getText());
  }
}
