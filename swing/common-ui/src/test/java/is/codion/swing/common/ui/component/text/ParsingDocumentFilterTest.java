/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ParsingDocumentFilterTest {

  @Test
  void test() throws BadLocationException {
    JTextField textField = new JTextField();
    AbstractDocument document = (AbstractDocument) textField.getDocument();
    Parser<String> parser = text -> new DefaultParseResult<>(text, text, true);
    Value.Validator<String> validator = value -> {
      if (!value.contains("42")) {
        throw new IllegalArgumentException();
      }
    };
    ParsingDocumentFilter<String> validationFilter = new ParsingDocumentFilter<>(parser);
    validationFilter.addValidator(validator);
    document.setDocumentFilter(validationFilter);
    document.insertString(0, "abc42bca", null);
    assertEquals("abc42bca", textField.getText());
    document.remove(0, 2);
    assertEquals("c42bca", textField.getText());
    document.replace(0, 1, "", null);
    assertEquals("42bca", textField.getText());
    assertThrows(IllegalArgumentException.class, () -> document.replace(0, 1, "", null));
    assertEquals("42bca", textField.getText());
  }
}
