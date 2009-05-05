/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.IntField;

public class IntBeanPropertyLink extends TextBeanPropertyLink {

  public IntBeanPropertyLink(final IntField intField, final Object owner, final String propertyName,
                             final Event propertyChangeEvent, final String text) {
    this(intField, owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE);
  }

  public IntBeanPropertyLink(final IntField intField, final Object owner, final String propertyName,
                             final Event propertyChangeEvent, final String text, final LinkType linkType) {
    super(intField, owner, propertyName, Integer.class, propertyChangeEvent, text, linkType, null);
    updateUI();
  }

  /** {@inheritDoc} */
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
