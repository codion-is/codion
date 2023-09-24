/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson.
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
