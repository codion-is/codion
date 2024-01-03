/*
 * Copyright (c) 2012 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.text.AbstractDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StringLengthValidatorTest {

  @Test
  void insert() {
    JTextArea textArea = new JTextArea();
    ParsingDocumentFilter<String> documentFilter = new ParsingDocumentFilter<>(ParsingDocumentFilter.STRING_PARSER);
    documentFilter.addValidator(new StringLengthValidator(10));
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentFilter);
    final String text8 = "12345678";
    textArea.setText(text8);
    assertEquals(text8, textArea.getText());
    textArea.insert("90", 8);
    final String text10 = "1234567890";
    assertEquals(text10, textArea.getText());
    assertThrows(IllegalArgumentException.class, () -> textArea.insert(text10, 10));
    assertEquals(text10, textArea.getText());
  }

  @Test
  void replace() {
    JTextArea textArea = new JTextArea();
    ParsingDocumentFilter<String> documentFilter = new ParsingDocumentFilter<>(ParsingDocumentFilter.STRING_PARSER);
    documentFilter.addValidator(new StringLengthValidator(10));
    ((AbstractDocument) textArea.getDocument()).setDocumentFilter(documentFilter);
    final String text8 = "12345678";
    textArea.setText(text8);
    textArea.replaceRange("90", 8, 8);
    final String text10 = "1234567890";
    assertEquals(text10, textArea.getText());
    assertThrows(IllegalArgumentException.class, () -> textArea.replaceRange("ab", 4, 4));
    assertEquals(text10, textArea.getText());
  }
}
