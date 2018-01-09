/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.junit.Test;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import static org.junit.Assert.assertEquals;

public final class ValidationDocumentFilterTest {

  @Test
  public void test() throws BadLocationException {
    final JTextField textField = new JTextField();
    final AbstractDocument document = (AbstractDocument) textField.getDocument();
    document.setDocumentFilter(new ValidationDocumentFilter() {
      @Override
      protected boolean isValid(final String text) {
        return text.contains("42");
      }
    });
    document.insertString(0, "abc42bca", null);
    assertEquals("abc42bca", textField.getText());
    document.remove(0, 2);
    assertEquals("c42bca", textField.getText());
    document.replace(0, 1, "", null);
    assertEquals("42bca", textField.getText());
    document.replace(0, 1, "", null);
    assertEquals("42bca", textField.getText());
  }
}
