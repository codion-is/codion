/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JList;

final class SingleSelectionListValue<V> extends AbstractComponentValue<V, JList<V>> {

  SingleSelectionListValue(final JList<V> list) {
    super(list);
    list.addListSelectionListener(e -> notifyValueChange());
  }

  @Override
  protected V getComponentValue(final JList<V> component) {
    return component.getSelectedValue();
  }

  @Override
  protected void setComponentValue(final JList<V> component, final V value) {
    component.setSelectedValue(value, true);
  }
}
