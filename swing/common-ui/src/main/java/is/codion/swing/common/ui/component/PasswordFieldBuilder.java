/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.TextFieldBuilder;

import javax.swing.JPasswordField;

/**
 * Builds a JPasswordField.
 */
public interface PasswordFieldBuilder extends TextFieldBuilder<String, JPasswordField, PasswordFieldBuilder> {

  /**
   * @param echoChar the echo char
   * @return this builder instance
   */
  PasswordFieldBuilder echoChar(char echoChar);
}
