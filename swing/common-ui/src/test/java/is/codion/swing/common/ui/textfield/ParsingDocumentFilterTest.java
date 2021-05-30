/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import static is.codion.swing.common.ui.textfield.Parser.parseResult;
import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ParsingDocumentFilterTest {

  @Test
  void test() throws BadLocationException {
    final JTextField textField = new JTextField();
    final AbstractDocument document = (AbstractDocument) textField.getDocument();
    final Parser<String> parser = text -> parseResult(text, text);
    final Value.Validator<String> validator = value -> {
      if (!value.contains("42")) {
        throw new IllegalArgumentException();
      }
    };
    final ParsingDocumentFilter<String> validationFilter = parsingDocumentFilter(parser, validator);
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
