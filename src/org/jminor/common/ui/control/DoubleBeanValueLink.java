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
    this(doubleField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
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
    this(doubleField, owner, propertyName, valueChangeEvent, linkType, false);
  }

  /**
   * Instantiates a new DoubleBeanValueLink.
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public DoubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent, final boolean usePrimitive) {
    this(doubleField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE, usePrimitive);
  }

  /**
   * Instantiates a new DoubleBeanValueLink.
   * @param doubleField the double field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, double instead of Double
   */
  public DoubleBeanValueLink(final DoubleField doubleField, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent, final LinkType linkType, final boolean usePrimitive) {
    super(doubleField, owner, propertyName, usePrimitive ? double.class : Double.class, valueChangeEvent, linkType);
  }

  /** {@inheritDoc} */
  @Override
  protected final Object getValueFromText(final String text) {
    if (text.isEmpty() && getValueClass().equals(double.class)) {
      return 0;
    }

    return Util.getDouble(text);
  }
}