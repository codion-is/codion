/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JList;

final class ListValue<T> extends AbstractComponentValue<T, JList<T>> {

  ListValue(JList<T> list) {
    super(list);
    list.addListSelectionListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue() {
    return component().getSelectedValue();
  }

  @Override
  protected void setComponentValue(T value) {
    component().setSelectedValue(value, true);
  }
}
