/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.item.Item;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.util.Objects;

final class SpinnerItemValue<V> extends AbstractComponentValue<V, JSpinner> {

  SpinnerItemValue(final JSpinner spinner) {
    super(spinner);
    if (!(spinner.getModel() instanceof SpinnerListModel)) {
      throw new IllegalArgumentException("Spinner model must be a SpinnerListModel");
    }
    spinner.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected V getComponentValue(final JSpinner component) {
    final Item<V> selectedValue = (Item<V>) component.getModel().getValue();

    return selectedValue == null ? null : selectedValue.getValue();
  }

  @Override
  protected void setComponentValue(final JSpinner component, final V value) {
    final SpinnerListModel model = (SpinnerListModel) component.getModel();
    model.getList().stream()
            .map(Item.class::cast)
            .filter(item -> Objects.equals(item.getValue(), value))
            .findFirst()
            .ifPresent(model::setValue);
  }
}
