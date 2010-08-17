/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.DoubleField;

/**
 * Binds a DoubleField to a double based bean property.
 */
public class DoubleBeanValueLink extends TextBeanValueLink {

  /**
   * Instantiates a new DoubleBeanValueLink.
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public DoubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent) {
    this(doubleField, owner, propertyName, valueChangeEvent,LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new DoubleBeanValueLink.
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public DoubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent, final LinkType linkType) {
    super(doubleField, owner, propertyName, Double.class, valueChangeEvent, linkType);
    updateUI();
  }

  /** {@inheritDoc} */
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