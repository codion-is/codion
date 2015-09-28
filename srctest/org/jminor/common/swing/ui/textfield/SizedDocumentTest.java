/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.textfield;

import org.junit.Test;

import javax.swing.JTextField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SizedDocumentTest {

  @Test
  public void test() {
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
    assertEquals("", txt.getText());

    document.setUpperCase(true);
    assertTrue(document.isUpperCase());

    txt.setText("hello");
    assertEquals("HELLO", txt.getText());

    document.setLowerCase(true);
    assertTrue(document.isLowerCase());
    txt.setText("HELLO");
    assertEquals("hello", txt.getText());
  }
}
