/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.DoubleField;

/**
 * Binds a DoubleField to a double based bean property.
 */
public class DoubleBeanPropertyLink extends TextBeanPropertyLink {

  public DoubleBeanPropertyLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                final Event propertyChangeEvent) {
    this(doubleField, owner, propertyName, propertyChangeEvent,LinkType.READ_WRITE);
  }

  public DoubleBeanPropertyLink(final DoubleField doubleField, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final LinkType linkType) {
    super(doubleField, owner, propertyName, Double.class, propertyChangeEvent, linkType);
    updateUI();
  }

  @Override
  protected Object getUIPropertyValue() {
    try {
      return Util.getDouble(getText());
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}