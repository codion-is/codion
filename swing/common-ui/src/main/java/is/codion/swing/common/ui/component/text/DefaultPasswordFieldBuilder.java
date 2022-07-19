/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

import javax.swing.JPasswordField;

final class DefaultPasswordFieldBuilder extends DefaultTextFieldBuilder<String, JPasswordField, PasswordFieldBuilder>
        implements PasswordFieldBuilder {

  private char echoChar;

  DefaultPasswordFieldBuilder(Value<String> linkedValue) {
    super(String.class, linkedValue);
  }

  @Override
  public PasswordFieldBuilder echoChar(char echoChar) {
    this.echoChar = echoChar;
    return this;
  }

  @Override
  protected JPasswordField createTextField() {
    JPasswordField passwordField = new JPasswordField();
    if (echoChar != 0) {
      passwordField.setEchoChar(echoChar);
    }

    return passwordField;
  }
}
