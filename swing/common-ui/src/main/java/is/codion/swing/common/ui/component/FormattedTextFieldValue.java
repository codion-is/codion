/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JFormattedTextField;

final class FormattedTextFieldValue<T> extends AbstractComponentValue<T, JFormattedTextField> {

  FormattedTextFieldValue(JFormattedTextField component) {
    super(component);
    component.addPropertyChangeListener("value", event -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(JFormattedTextField component) {
    return (T) component.getValue();
  }

  @Override
  protected void setComponentValue(JFormattedTextField component, T value) {
    component.setValue(value);
  }
}
