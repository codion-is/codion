/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.JTextField;

final class CharacterFieldValue extends AbstractTextComponentValue<Character, JTextField> {

  CharacterFieldValue(JTextField textField, UpdateOn updateOn) {
    super(textField, null, updateOn);
  }

  @Override
  protected Character getComponentValue() {
    String string = component().getText();

    return string.isEmpty() ? null : string.charAt(0);
  }

  @Override
  protected void setComponentValue(Character value) {
    component().setText(value == null ? "" : String.valueOf(value));
  }
}
