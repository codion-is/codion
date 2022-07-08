/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.slider;

import is.codion.swing.common.ui.component.AbstractComponentValue;

import javax.swing.JSlider;

final class IntegerSliderValue extends AbstractComponentValue<Integer, JSlider> {

  IntegerSliderValue(JSlider slider) {
    super(slider, 0);
    slider.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue() {
    return getComponent().getValue();
  }

  @Override
  protected void setComponentValue(Integer value) {
    getComponent().setValue(value == null ? 0 : value);
  }
}
