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
public class IntBeanValueLink extends TextBeanValueLink {

  public IntBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                          final Event valueChangeEvent) {
    this(intField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  public IntBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                          final Event valueChangeEvent, final LinkType linkType) {
    super(intField, owner, propertyName, Integer.class, valueChangeEvent, linkType);
    updateUI();
  }

  @Override
  protected Object getUIValue() {
    try {
      return Util.getInt(getText());
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
