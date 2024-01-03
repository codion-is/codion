/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
