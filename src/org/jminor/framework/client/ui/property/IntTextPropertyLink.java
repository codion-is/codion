/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

public class IntTextPropertyLink extends TextPropertyLink {

  private int minValue = -Integer.MAX_VALUE;
  private int maxValue = Integer.MAX_VALUE;

  public IntTextPropertyLink(final EntityModel entityModel, final Property property, final IntField textField,
                             final String caption, final boolean immediateUpdate, final LinkType linkType,
                             final State enableState) {
    super(entityModel, property, textField, caption, immediateUpdate, linkType, null, enableState);
  }

  /** {@inheritDoc} */
  protected void initConstants() {
    setMinValue(-Integer.MAX_VALUE);
    setMaxValue(Integer.MAX_VALUE);
  }

  /** {@inheritDoc} */
  public void checkValidValue() {
    final double value = (Double) getPropertyValue();
    if (value < getMinValue() || value > getMaxValue())
      throw new RuntimeException("You must enter a value between " + getMinValue() + " and " + getMaxValue());
  }

  /**
   * @param min Value to set for property 'minValue'.
   */
  public void setMinValue(final int min) {
    this.minValue = min;
  }

  /**
   * @param max Value to set for property 'maxValue'.
   */
  public void setMaxValue(final int max) {
    this.maxValue = max;
  }

  /**
   * @return Value for property 'minValue'.
   */
  public int getMinValue() {
    return minValue;
  }

  /**
   * @return Value for property 'maxValue'.
   */
  public int getMaxValue() {
    return maxValue;
  }

  /** {@inheritDoc} */
  protected Object valueFromText(final String text) {
    try {
      return text.length() > 0 ? Integer.parseInt(text) : null;
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
