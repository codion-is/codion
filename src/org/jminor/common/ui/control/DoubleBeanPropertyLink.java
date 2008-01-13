/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.Constants;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.ui.textfield.DoubleField;

public class DoubleBeanPropertyLink extends TextBeanPropertyLink {

  public DoubleBeanPropertyLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text) {
    this(doubleField, owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE, null);
  }

  public DoubleBeanPropertyLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text, final LinkType linkType,
                                final State enabledState) {
    super(doubleField, owner, propertyName, double.class, propertyChangeEvent, text, linkType, null, enabledState);
    refreshUI();
  }

  /** {@inheritDoc} */
  protected Object textToValue() {
    final String text = getText();
    try {
      return text.length() > 0 ? Double.parseDouble(text) : Constants.DOUBLE_NULL_VALUE;
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}