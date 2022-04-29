/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JFormattedTextField;

final class MaskedTextFieldValue<T> extends AbstractComponentValue<T, JFormattedTextField> {

  MaskedTextFieldValue(JFormattedTextField component) {
    super(component);
    component.addPropertyChangeListener("value", event -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(JFormattedTextField component) {
    return (T) component.getValue();
  }

  @Override
  protected void setComponentValue(JFormattedTextField component, T value) {
    if (value == null) {
      // otherwise the caret goes all the way to the
      // end the next time the field gains focus
      component.setText("");
    }
    else {
      component.setValue(value);
    }
  }
}