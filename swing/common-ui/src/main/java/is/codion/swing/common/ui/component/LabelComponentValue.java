/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JLabel;

final class LabelComponentValue<T> extends AbstractComponentValue<T, JLabel> {

  LabelComponentValue(JLabel component) {
    super(component);
  }

  @Override
  protected T getComponentValue() {
    return null;
  }

  @Override
  protected void setComponentValue(T value) {
    component().setText(value == null ? "" : value.toString());
  }
}
