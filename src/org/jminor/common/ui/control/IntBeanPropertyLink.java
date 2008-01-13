/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.Constants;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.ui.textfield.IntField;

public class IntBeanPropertyLink extends TextBeanPropertyLink {

  public IntBeanPropertyLink(final IntField intField, final Object owner, final String propertyName,
                             final Event propertyChangeEvent, final String text) {
    this(intField, owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE, null);
  }

  public IntBeanPropertyLink(final IntField intField, final Object owner, final String propertyName,
                             final Event propertyChangeEvent, final String text, final LinkType linkType,
                             final State enabledState) {
    super(intField, owner, propertyName, int.class, propertyChangeEvent, text, linkType, null, enabledState);
    refreshUI();
  }

  /** {@inheritDoc} */
  protected Object textToValue() {
    final String text = getText();
    try {
      return text.length() > 0 ? Integer.parseInt(text) : Constants.INT_NULL_VALUE;
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
