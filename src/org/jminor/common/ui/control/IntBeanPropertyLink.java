/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

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
    super(intField, owner, propertyName, Integer.class, propertyChangeEvent, text, linkType, null, enabledState);
    refreshUI();
  }

  /** {@inheritDoc} */
  protected Object textToValue() {
    final String text = getText();
    try {
      return text.length() > 0 ? Integer.parseInt(text) : null;
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
