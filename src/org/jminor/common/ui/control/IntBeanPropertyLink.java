/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.IntField;

/**
 * Binds a DoubleField to a int based bean property.
 */
public class IntBeanPropertyLink extends TextBeanPropertyLink {

  public IntBeanPropertyLink(final IntField intField, final Object owner, final String propertyName,
                             final Event propertyChangeEvent) {
    this(intField, owner, propertyName, propertyChangeEvent, LinkType.READ_WRITE);
  }

  public IntBeanPropertyLink(final IntField intField, final Object owner, final String propertyName,
                             final Event propertyChangeEvent, final LinkType linkType) {
    super(intField, owner, propertyName, Integer.class, propertyChangeEvent, linkType);
    updateUI();
  }

  @Override
  protected Object getUIPropertyValue() {
    try {
      return Util.getInt(getText());
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
