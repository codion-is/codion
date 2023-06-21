/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.slider;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JSlider;

final class IntegerSliderValue extends AbstractComponentValue<Integer, JSlider> {

  IntegerSliderValue(JSlider slider) {
    super(slider, 0);
    slider.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue() {
    return component().getValue();
  }

  @Override
  protected void setComponentValue(Integer value) {
    component().setValue(value == null ? 0 : value);
  }
}
