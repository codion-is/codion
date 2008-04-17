/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

public class DoubleTextPropertyLink extends TextPropertyLink {

  private double minValue = Double.NEGATIVE_INFINITY;
  private double maxValue = Double.POSITIVE_INFINITY;

  public DoubleTextPropertyLink(final EntityModel entityModel, final Property property,
                                final DoubleField textField, final boolean immediateUpdate,
                                final LinkType linkType, final State enableState) {
    super(entityModel, property, textField, immediateUpdate, linkType, null, enableState);
  }

  /** {@inheritDoc} */
  protected void initConstants() {
    setMinValue(Double.NEGATIVE_INFINITY);
    setMaxValue(Double.POSITIVE_INFINITY);
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
  public void setMinValue(final double min) {
    this.minValue = min;
  }

  /**
   * @param max Value to set for property 'maxValue'.
   */
  public void setMaxValue(final double max) {
    this.maxValue = max;
  }

  /**
   * @return Value for property 'minValue'.
   */
  public double getMinValue() {
    return minValue;
  }

  /**
   * @return Value for property 'maxValue'.
   */
  public double getMaxValue() {
    return maxValue;
  }

  /** {@inheritDoc} */
  protected Object valueFromText(final String text) {
    try {
      if (text != null && text.equals("-"))
        return -1;

      return text.length() > 0 ? Double.parseDouble(text) : null;
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
