/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import javax.swing.ButtonModel;

final class BooleanButtonModelValue extends AbstractComponentValue<Boolean, ButtonModel> {

  BooleanButtonModelValue(final ButtonModel buttonModel) {
    super(buttonModel, false);
    buttonModel.addItemListener(e -> notifyValueChange(get()));
  }

  @Override
  public Boolean get() {
    return getComponent().isSelected();
  }

  @Override
  protected void setInternal(final Boolean value) {
     getComponent().setSelected(value != null && value);
  }
}
