/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.IntField;

import java.text.NumberFormat;

/**
 * Binds a DoubleField to a int based property.
 */
final class IntValueLink extends TextValueLink {

  private final boolean usePrimitive;

  /**
   * Instantiates a new IntValueLink.
   * @param intField the int field to link with the value
   * @param modelValue the model value
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  IntValueLink(final IntField intField, final ModelValue modelValue, final LinkType linkType, final boolean usePrimitive,
               final NumberFormat format) {
    super(intField, modelValue, linkType, format, true);
    this.usePrimitive = usePrimitive;
  }

  /** {@inheritDoc} */
  @Override
  protected Object getValueFromText(final String text) {
    if (text.isEmpty() && usePrimitive) {
      return 0;
    }

    return Util.getInt(text);
  }
}
