/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JList;

final class ListValue<T> extends AbstractComponentValue<T, JList<T>> {

  ListValue(final JList<T> list) {
    super(list);
    list.addListSelectionListener(e -> notifyValueChange());
  }

  @Override
  protected T getComponentValue(final JList<T> component) {
    return component.getSelectedValue();
  }

  @Override
  protected void setComponentValue(final JList<T> component, final T value) {
    component.setSelectedValue(value, true);
  }
}
