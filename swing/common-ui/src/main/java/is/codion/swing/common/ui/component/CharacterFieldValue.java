/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JTextField;

final class CharacterFieldValue extends AbstractTextComponentValue<Character, JTextField> {

  CharacterFieldValue(final JTextField textField, final UpdateOn updateOn) {
    super(textField, null, updateOn);
  }

  @Override
  protected Character getComponentValue(final JTextField component) {
    String string = component.getText();

    return string.isEmpty() ? null : string.charAt(0);
  }

  @Override
  protected void setComponentValue(final JTextField component, final Character value) {
    component.setText(value == null ? "" : String.valueOf(value));
  }
}
