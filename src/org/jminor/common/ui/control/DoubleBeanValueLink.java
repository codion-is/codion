/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.DoubleField;

import java.text.NumberFormat;

/**
 * Binds a DoubleField to a double based bean property.
 */
final class DoubleBeanValueLink extends TextBeanValueLink {

  private final boolean usePrimitive;

  /**
   * Instantiates a new DoubleBeanValueLink.
   * @param doubleField the double field to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  DoubleBeanValueLink(final DoubleField doubleField, final ModelValue modelValue, final LinkType linkType, final boolean usePrimitive,
                      final NumberFormat format) {
    super(doubleField, modelValue, linkType, format, true);
    this.usePrimitive = usePrimitive;
  }

  /** {@inheritDoc} */
  @Override
  protected Object getValueFromText(final String text) {
    if (text.isEmpty() && usePrimitive) {
      return 0;
    }

    return Util.getDouble(text);
  }
}