/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.DoubleField;

public class DoubleBeanPropertyLink extends TextBeanPropertyLink {

  public DoubleBeanPropertyLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text) {
    this(doubleField, owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE);
  }

  public DoubleBeanPropertyLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text, final LinkType linkType) {
    super(doubleField, owner, propertyName, Double.class, propertyChangeEvent, text, linkType, null);
    updateUI();
  }

  /** {@inheritDoc} */
  protected Object getUIPropertyValue() {
    try {
      return Util.getDouble(getText());
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}