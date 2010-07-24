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
public class DoubleBeanValueLink extends TextBeanValueLink {

  public DoubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                             final Event valueChangeEvent) {
    this(doubleField, owner, propertyName, valueChangeEvent,LinkType.READ_WRITE);
  }

  public DoubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                             final Event valueChangeEvent, final LinkType linkType) {
    super(doubleField, owner, propertyName, Double.class, valueChangeEvent, linkType);
    updateUI();
  }

  @Override
  protected final Object getUIValue() {
    try {
      return Util.getDouble(getText());
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}