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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import javax.swing.JPasswordField;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JPasswordField.
 */
public interface PasswordFieldBuilder extends TextFieldBuilder<String, JPasswordField, PasswordFieldBuilder> {

  /**
   * @param echoChar the echo char
   * @return this builder instance
   * @see JPasswordField#setEchoChar(char)
   */
  PasswordFieldBuilder echoChar(char echoChar);

  /**
   * @return a new JPasswordField
   */
  static PasswordFieldBuilder builder() {
    return new DefaultPasswordFieldBuilder(null);
  }

  /**
   * @param linkedValue the value to link to the component
   * @return a new JPasswordField
   */
  static PasswordFieldBuilder builder(Value<String> linkedValue) {
    return new DefaultPasswordFieldBuilder(requireNonNull(linkedValue));
  }
}
